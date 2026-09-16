package com.odyssey.api.booking.confirmation;

import com.odyssey.api.agent.Agent;
import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.booking.BookingRequest;
import com.odyssey.api.booking.BookingRequestStatus;
import com.odyssey.api.exception.ResourceNotFoundException;
import com.odyssey.api.need.Need;
import com.odyssey.api.payment.PaymentRepository;
import com.odyssey.api.payment.PaymentStatus;
import com.odyssey.api.quote.Quote;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.quote.QuoteStatus;
import com.odyssey.api.trip.Trip;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	private static final Long AGENT_ID = 7L;
	private static final Long QUOTE_ID = 42L;
	private static final Long BOOKING_ID = 500L;
	private static final Long TRIP_ID = 10L;
	private static final String AUTH0_SUBJECT = "auth0|agent-a";

	@Mock private BookingRepository bookingRepository;
	@Mock private QuoteRepository quoteRepository;
	@Mock private PaymentRepository paymentRepository;
	@Mock private FakeBookingProvider bookingProvider;
	@Mock private AgentRepository agentRepository;

	private BookingService bookingService;

	@BeforeEach
	void setUp() {
		bookingService = new BookingService(
			bookingRepository,
			quoteRepository,
			paymentRepository,
			bookingProvider,
			agentRepository
		);
	}

	@Test
	void createBookingResolvesAssignedAgentFromAuth0Subject() {
		Agent agent = agent(AGENT_ID);
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		when(agentRepository.findByAuth0Subject(AUTH0_SUBJECT))
			.thenReturn(Optional.of(agent));
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
		when(paymentRepository.existsByTripIdAndStatus(TRIP_ID, PaymentStatus.PAID))
			.thenReturn(true);
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
	void createBookingRejectsUnpaidTripAssistanceFee() {
		Quote quote = acceptedQuoteAssignedTo(AGENT_ID);
		when(quoteRepository.findById(QUOTE_ID)).thenReturn(Optional.of(quote));
		when(paymentRepository.existsByTripIdAndStatus(TRIP_ID, PaymentStatus.PAID))
			.thenReturn(false);

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
		when(paymentRepository.existsByTripIdAndStatus(TRIP_ID, PaymentStatus.PAID))
			.thenReturn(true);

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

		assertThrows(
			IllegalStateException.class,
			() -> bookingService.confirmBooking(BOOKING_ID, AGENT_ID)
		);
		verify(bookingProvider, never()).confirmBooking(any());
	}

	@Test
	void confirmBookingCompletesBookingRequest() {
		Booking booking = pendingBookingAssignedTo(AGENT_ID);
		booking.setProviderReference("PNR-123");
		when(bookingRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
		when(bookingProvider.confirmBooking(booking)).thenReturn("CONF-ABC");
		when(bookingRepository.save(booking)).thenReturn(booking);

		BookingResponse response = bookingService.confirmBooking(BOOKING_ID, AGENT_ID);

		assertEquals(BookingStatus.CONFIRMED, response.status());
		assertEquals("CONF-ABC", response.providerConfirmationId());
		assertEquals(
			BookingRequestStatus.COMPLETED,
			booking.getQuote().getBookingRequest().getStatus()
		);
	}

	private Quote acceptedQuoteAssignedTo(Long agentId) {
		Trip trip = new Trip();
		ReflectionTestUtils.setField(trip, "id", TRIP_ID);

		Need need = new Need();
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
