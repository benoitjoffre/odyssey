package com.odyssey.api.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.odyssey.api.agent.AgentRepository;
import com.odyssey.api.quote.QuoteRepository;
import com.odyssey.api.traveler.Traveler;
import com.odyssey.api.traveler.TravelerRepository;
import com.odyssey.api.trip.TripRepository;

@ExtendWith(MockitoExtension.class)
class ClientEmailEventListenerTest {

    @Mock
    private TravelerRepository travelerRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OdysseyEmailRenderer emailRenderer;

    private ClientEmailEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ClientEmailEventListener(
            travelerRepository,
            agentRepository,
            quoteRepository,
            tripRepository,
            emailService,
            emailRenderer
        );
    }

    @Test
    void onboardingCompletedSendsWelcomeEmailToTraveler() {
        Traveler traveler = new Traveler("Alice", "Martin", "alice@example.com");
        traveler.setPreferredLanguage("fr");
        when(travelerRepository.findById(42L)).thenReturn(Optional.of(traveler));

        EmailMessage message = new EmailMessage(
            "alice@example.com",
            "Bienvenue chez Odyssey",
            "<html>...</html>",
            "Bonjour Alice"
        );
        when(emailRenderer.travelerOnboardingCompleted(
            "alice@example.com",
            "fr",
            "Alice"
        )).thenReturn(message);

        listener.onTravelerOnboardingCompleted(
            new TravelerOnboardingCompletedEvent(42L)
        );

        verify(emailService).sendEmail(message);
    }
}