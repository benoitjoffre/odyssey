package com.odyssey.api.intent.recommendation;

import com.odyssey.api.experience.ExperienceCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AIIntentAnalyzerTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;
    private IntentAnalyzer deterministicAnalyzer;
    private AIIntentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);
        deterministicAnalyzer = mock(IntentAnalyzer.class);
        analyzer = new AIIntentAnalyzer(chatClient, deterministicAnalyzer);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
    }

    @Test
    void returnsValidStructuredAiResult() {
        when(responseSpec.entity(AIIntentResult.class)).thenReturn(
            new AIIntentResult("dance", "salsa", "Cuba")
        );

        AnalyzedIntent result = analyzer.analyze(
            "Je veux apprendre la salsa à Cuba"
        );

        assertEquals(ExperienceCategory.DANCE, result.category());
        assertEquals("salsa", result.activity());
        assertEquals("Cuba", result.destination());
        verifyNoInteractions(deterministicAnalyzer);
    }

    @Test
    void fallsBackWhenAiReturnsUnknownCategory() {
        String text = "Je veux découvrir une activité locale";
        AnalyzedIntent fallback = new AnalyzedIntent(null, null, null);
        when(responseSpec.entity(AIIntentResult.class)).thenReturn(
            new AIIntentResult("PARTY", "fête", null)
        );
        when(deterministicAnalyzer.analyze(text)).thenReturn(fallback);

        assertEquals(fallback, analyzer.analyze(text));
    }

    @Test
    void fallsBackWhenAiCallFails() {
        String text = "Je veux faire du surf au Portugal";
        AnalyzedIntent fallback = new AnalyzedIntent(
            ExperienceCategory.SURF,
            "surf",
            "Portugal"
        );
        when(requestSpec.call()).thenThrow(new RuntimeException("OpenAI unavailable"));
        when(deterministicAnalyzer.analyze(text)).thenReturn(fallback);

        assertEquals(fallback, analyzer.analyze(text));
    }

    @Test
    void acceptsNullFieldsFromAiResult() {
        when(responseSpec.entity(AIIntentResult.class)).thenReturn(
            new AIIntentResult(null, null, null)
        );

        AnalyzedIntent result = analyzer.analyze(
            "Je veux partir au soleil et rencontrer du monde"
        );

        assertNull(result.category());
        assertNull(result.activity());
        assertNull(result.destination());
        verifyNoInteractions(deterministicAnalyzer);
    }

    @Test
    void blankInputDoesNotCallAi() {
        AIIntentAnalyzer blankAnalyzer = new AIIntentAnalyzer(
            mock(ChatClient.class),
            deterministicAnalyzer
        );

        assertEquals(
            new AnalyzedIntent(null, null, null),
            blankAnalyzer.analyze("  ")
        );
        verifyNoInteractions(deterministicAnalyzer);
    }
}