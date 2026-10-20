package com.odyssey.api.booking.confirmation;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestRepository;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.trip.Trip;
import com.odyssey.api.trip.TripRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.outbox.OutboxEventRepository;
import com.odyssey.api.outbox.OutboxEvent;
import com.odyssey.api.event.ProviderPaymentRequiredEvent;
import com.odyssey.api.event.TripBookingsConfirmedEvent;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	private static final Long AGENT_ID = 7L;
	private static final Long QUOTE_ID = 42L;
	private static final Long BOOKING_ID = 500L;
	private static final String AUTH0_SUBJECT = "auth0|agent-a";

	@Mock private BookingRepository bookingRepository;
	@Mock private QuoteRepository quoteRepository;
	@Mock private FakeBookingProvider bookingProvider;
	@Mock private AgentRepository agentRepository;
	@Mock private OutboxEventRepository outboxEventRepository;
	@Mock private BookingRequestRepository bookingRequestRepository;
	@Mock private TripRepository tripRepository;

	private BookingService bookingService;

	@BeforeEach
	void setUp() {
		bookingService = new BookingService(
			bookingRepository,
			quoteRepository,
			bookingProvider,
			agentRepository,
			outboxEventRepository,
			new ObjectMapper(),
			bookingRequestRepository,
			tripRepository
		);
	}

	@Test
	void createBookingResolvesAssignedAgentFromAuth0Subject() {
		Agent agent = agent(AGENT_ID);
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		when(agentRepository.findByAuth0Subject(AUTH0_SUBJECT))
			.thenReturn(Optional.of(agent));
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
		when(bookingRepository.existsByQuoteId(QUOTE_ID)).thenReturn(false);
		when(bookingRepository.save(any(Booking.class)))
			.thenAnswer(invocation -> {
				Booking booking = invocation.getArgument(0);
				ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
				return booking;
			});

		BookingResponse response = bookingService.createBooking(QUOTE_ID, AUTH0_SUBJECT);

		assertEquals(BOOKING_ID, response.id());
		assertEquals(BookingStatus.PENDING, response.status());
		assertEquals(ProviderPaymentStatus.NOT_REQUIRED_YET, response.providerPaymentStatus());
		verify(agentRepository).findByAuth0Subject(AUTH0_SUBJECT);
	}

	@Test
	void createBookingRejectsUnknownAuthenticatedAgent() {
		when(agentRepository.findByAuth0Subject(AUTH0_SUBJECT))
			.thenReturn(Optional.empty());

		assertThrows(
			ResourceNotFoundException.class,
			() -> bookingService.createBooking(QUOTE_ID, AUTH0_SUBJECT)
		);
		verify(quoteRepository, never()).findById(any());
	}

	@Test
	void createBookingAllowsAcceptedQuoteWithoutOdysseyPayment() {
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
		when(bookingRepository.existsByQuoteId(QUOTE_ID)).thenReturn(false);
		when(bookingRepository.save(any(Booking.class)))
			.thenAnswer(invocation -> {
				Booking booking = invocation.getArgument(0);
				ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
				return booking;
			});

		BookingResponse response = bookingService.createBooking(QUOTE_ID, AGENT_ID);

		assertEquals(BOOKING_ID, response.id());
		assertEquals(BookingStatus.PENDING, response.status());
		assertEquals(ProviderPaymentStatus.NOT_REQUIRED_YET, response.providerPaymentStatus());
	}

	@Test
	void createBookingRejectsQuoteThatIsNotAccepted() {
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		quote.setStatus(QuoteStatus.SENT);
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

		assertThrows(
			IllegalArgumentException.class,
			() -> bookingService.createBooking(QUOTE_ID, AGENT_ID)
		);
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void createBookingRejectsAnotherAgent() {
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));

		assertThrows(
			IllegalArgumentException.class,
			() -> bookingService.createBooking(QUOTE_ID, 999L)
		);
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void updateProviderDetailsStoresProviderData() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(booking)).thenReturn(booking);

		BookingResponse response = bookingService.updateProviderDetails(
			BOOKING_ID,
			AGENT_ID,
			"PNR-123",
			"https://provider.example.com/pay/PNR-123",
			ProviderPaymentStatus.PAYMENT_REQUIRED
		);

		assertEquals("PNR-123", response.providerReference());
		assertEquals("https://provider.example.com/pay/PNR-123", response.providerPaymentUrl());
		assertEquals(ProviderPaymentStatus.PAYMENT_REQUIRED, response.providerPaymentStatus());
		var event = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(event.capture());
		assertEquals("PROVIDER_PAYMENT_REQUIRED", event.getValue().getEventType());
		var payload = new ObjectMapper().readValue(event.getValue().getPayload(), ProviderPaymentRequiredEvent.class);
		assertEquals(11L, payload.travelerId());
		assertEquals(10L, payload.tripId());
	}

	@Test
	void unchangedProviderInstructionsDoNotSendAnotherEmail() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		booking.setProviderPaymentUrl("https://provider.example.com/pay/PNR-123");
		booking.setProviderPaymentStatus(ProviderPaymentStatus.PAYMENT_REQUIRED);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(booking)).thenReturn(booking);
		bookingService.updateProviderDetails(BOOKING_ID, AGENT_ID, "PNR-123",
			"https://provider.example.com/pay/PNR-123", ProviderPaymentStatus.PAYMENT_REQUIRED);
		verify(outboxEventRepository, never()).save(any());
	}

	@Test
	void providerMarkedPaidDoesNotSendPaymentInstructions() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(bookingRepository.save(booking)).thenReturn(booking);
		bookingService.updateProviderDetails(BOOKING_ID, AGENT_ID, "PNR-123",
			"https://provider.example.com/pay/PNR-123", ProviderPaymentStatus.PAID_TO_PROVIDER);
		verify(outboxEventRepository, never()).save(any());
	}

	@Test
	void updateProviderDetailsRejectsAnotherAgent() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

		assertThrows(
			IllegalArgumentException.class,
			() -> bookingService.updateProviderDetails(
				BOOKING_ID,
				999L,
				"PNR-123",
				null,
				ProviderPaymentStatus.PAYMENT_REQUIRED
			)
		);
		verify(bookingRepository, never()).save(any());
	}

	@Test
	void confirmBookingRejectsMissingProviderReference() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		lockTrip(booking);

		assertThrows(
			IllegalStateException.class,
			() -> bookingService.confirmBooking(BOOKING_ID, AGENT_ID)
		);
		verify(bookingProvider, never()).confirmBooking(any());
	}

	@Test
	void confirmingLastActiveBookingSendsOneTripConfirmation() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		booking.setProviderReference("PNR-123");
		BookingRequest request = booking.getQuote().getBookingRequest();
		Booking otherConfirmed = otherBooking(BookingStatus.CONFIRMED);
		BookingRequest cancelled = new BookingRequest();
		ReflectionTestUtils.setField(cancelled, "id", 30L);
		cancelled.setStatus(BookingRequestStatus.CANCELLED);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		lockTrip(booking);
		when(bookingProvider.confirmBooking(booking)).thenReturn("CONF-ABC");
		when(bookingRepository.save(booking)).thenReturn(booking);
		when(bookingRequestRepository.findByNeedTripId(10L))
			.thenReturn(List.of(request, otherConfirmed.getQuote().getBookingRequest(), cancelled));
		when(bookingRepository.findByQuoteBookingRequestId(9L)).thenReturn(Optional.of(booking));
		when(bookingRepository.findByQuoteBookingRequestId(20L)).thenReturn(Optional.of(otherConfirmed));

		BookingResponse response = bookingService.confirmBooking(BOOKING_ID, AGENT_ID);

		assertEquals(BookingStatus.CONFIRMED, response.status());
		assertEquals("CONF-ABC", response.providerConfirmationId());
		assertEquals(BookingRequestStatus.COMPLETED, request.getStatus());
		var events = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository, times(2)).save(events.capture());
		assertEquals("BOOKING_CONFIRMED", events.getAllValues().get(0).getEventType());
		assertEquals("TRIP_BOOKINGS_CONFIRMED", events.getAllValues().get(1).getEventType());
		var payload = new ObjectMapper().readValue(events.getAllValues().get(1).getPayload(),
			TripBookingsConfirmedEvent.class);
		assertEquals(10L, payload.tripId());
		assertEquals(11L, payload.travelerId());
		assertEquals(2, payload.bookingCount());
	}

	@Test
	void partialConfirmationDoesNotSendTripConfirmation() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		booking.setProviderReference("PNR-123");
		Booking otherPending = otherBooking(BookingStatus.PENDING);
		BookingRequest notBooked = new BookingRequest();
		ReflectionTestUtils.setField(notBooked, "id", 40L);
		notBooked.setStatus(BookingRequestStatus.IN_PROGRESS);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		lockTrip(booking);
		when(bookingProvider.confirmBooking(booking)).thenReturn("CONF-ABC");
		when(bookingRepository.save(booking)).thenReturn(booking);
		when(bookingRequestRepository.findByNeedTripId(10L)).thenReturn(List.of(
			booking.getQuote().getBookingRequest(), otherPending.getQuote().getBookingRequest(), notBooked));
		when(bookingRepository.findByQuoteBookingRequestId(9L)).thenReturn(Optional.of(booking));
		when(bookingRepository.findByQuoteBookingRequestId(20L)).thenReturn(Optional.of(otherPending));

		bookingService.confirmBooking(BOOKING_ID, AGENT_ID);

		var events = ArgumentCaptor.forClass(OutboxEvent.class);
		verify(outboxEventRepository).save(events.capture());
		assertEquals("BOOKING_CONFIRMED", events.getValue().getEventType());
	}

	@Test
	void requestWithoutBookingBlocksTripConfirmation() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		booking.setProviderReference("PNR-123");
		BookingRequest notBooked = new BookingRequest();
		ReflectionTestUtils.setField(notBooked, "id", 40L);
		notBooked.setStatus(BookingRequestStatus.REQUESTED);
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		lockTrip(booking);
		when(bookingProvider.confirmBooking(booking)).thenReturn("CONF-ABC");
		when(bookingRepository.save(booking)).thenReturn(booking);
		when(bookingRequestRepository.findByNeedTripId(10L))
			.thenReturn(List.of(notBooked, booking.getQuote().getBookingRequest()));
		when(bookingRepository.findByQuoteBookingRequestId(40L)).thenReturn(Optional.empty());

		bookingService.confirmBooking(BOOKING_ID, AGENT_ID);

		verify(outboxEventRepository, times(1)).save(any());
	}

	private void lockTrip(Booking booking) {
		Trip trip = booking.getQuote().getBookingRequest().getNeed().getTrip();
		when(tripRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(trip));
	}

	private Booking otherBooking(BookingStatus status) {
		BookingRequest request = new BookingRequest();
		ReflectionTestUtils.setField(request, "id", 20L);
		request.setStatus(status == BookingStatus.CONFIRMED
			? BookingRequestStatus.COMPLETED : BookingRequestStatus.IN_PROGRESS);
		Quote quote = new Quote();
		quote.setBookingRequest(request);
		Booking booking = new Booking();
		booking.setQuote(quote);
		booking.setStatus(status);
		return booking;
	}

	private Quote acceptedQuoteAssignedTo(Long agentId) {
		Need need = new Need();
		Trip trip = new Trip();
		ReflectionTestUtils.setField(trip, "id", 10L);
		trip.setTitle("Lisbonne");
		Traveler traveler = new Traveler("Alice", "Martin", "alice@example.com");
		traveler.setId(11L);
		trip.setTraveler(traveler);
		need.setTrip(trip);

		BookingRequest bookingRequest = new BookingRequest();
		ReflectionTestUtils.setField(bookingRequest, "id", 9L);
		bookingRequest.setNeed(need);
		bookingRequest.setAssignedAgent(agent(agentId));

		Quote quote = new Quote();
		ReflectionTestUtils.setField(quote, "id", QUOTE_ID);
		quote.setBookingRequest(bookingRequest);
		quote.setStatus(QuoteStatus.ACCEPTED);
		return quote;
	}

	private Booking pendingBookingAssignedTo(Long agentId) {
		Booking booking = new Booking();
		ReflectionTestUtils.setField(booking, "id", BOOKING_ID);
		booking.setQuote(acceptedQuoteAssignedTo(agentId));
		booking.setStatus(BookingStatus.PENDING);
		booking.setCreatedAt(Instant.now());
		booking.setProviderPaymentStatus(ProviderPaymentStatus.NOT_REQUIRED_YET);
		return booking;
	}

	private Agent agent(Long id) {
		Agent agent = new Agent();
		ReflectionTestUtils.setField(agent, "id", id);
		return agent;
	}
}
