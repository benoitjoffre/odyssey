package com.odyssey.api.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.odyssey.api.destination.Destination;
import com.odyssey.api.destination.DestinationRepository;
import com.odyssey.api.experience.Experience;
import com.odyssey.api.experience.ExperienceCategory;
import com.odyssey.api.experience.ExperienceRepository;
import com.odyssey.api.travelevent.TravelEvent;
import com.odyssey.api.travelevent.TravelEventRepository;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DevDataSeeder.class);

    private final DestinationRepository destinationRepository;
    private final ExperienceRepository experienceRepository;
    private final TravelEventRepository travelEventRepository;

    public DevDataSeeder(
        DestinationRepository destinationRepository,
        ExperienceRepository experienceRepository,
        TravelEventRepository travelEventRepository
    ) {
        this.destinationRepository = destinationRepository;
        this.experienceRepository = experienceRepository;
        this.travelEventRepository = travelEventRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int createdDestinations = 0;
        int createdExperiences = 0;
        int createdEvents = 0;

        SeededDestination bordeaux = createdOrExistingDestination("Bordeaux", "France", "FR");
        SeededDestination carcassonne = createdOrExistingDestination("Carcassonne", "France", "FR");
        SeededDestination cauterets = createdOrExistingDestination("Cauterets", "France", "FR");
        SeededDestination barcelona = createdOrExistingDestination("Barcelona", "Espagne", "ES");
        SeededDestination sevilla = createdOrExistingDestination("Sevilla", "Espagne", "ES");
        SeededDestination huesca = createdOrExistingDestination("Huesca", "Espagne", "ES");

        createdDestinations += countCreated(bordeaux);
        createdDestinations += countCreated(carcassonne);
        createdDestinations += countCreated(cauterets);
        createdDestinations += countCreated(barcelona);
        createdDestinations += countCreated(sevilla);
        createdDestinations += countCreated(huesca);

        SeededExperience bordeauxExperience = createdOrExistingExperience(
            "Bordeaux entre vignobles et gastronomie",
            "Découvrez Bordeaux à travers ses quartiers historiques, sa gastronomie et ses vignobles. Entre dégustations, patrimoine et balades au cœur de la ville, profitez d'une immersion dans l'art de vivre bordelais.",
            bordeaux.destination(),
            ExperienceCategory.FOOD,
            3
        );
        SeededExperience carcassonneExperience = createdOrExistingExperience(
            "Carcassonne, voyage au cœur du Moyen Âge",
            "Plongez dans l'histoire de Carcassonne et découvrez sa célèbre cité médiévale, ses remparts et son patrimoine.",
            carcassonne.destination(),
            ExperienceCategory.CULTURE,
            2
        );
        SeededExperience cauteretsExperience = createdOrExistingExperience(
            "Aventure dans les Pyrénées",
            "Partez à la découverte des Pyrénées françaises entre montagnes, lacs et sentiers.",
            cauterets.destination(),
            ExperienceCategory.ADVENTURE,
            4
        );
        SeededExperience barcelonaExperience = createdOrExistingExperience(
            "Barcelone entre architecture et tapas",
            "Explorez Barcelone à travers son architecture, ses quartiers emblématiques et sa gastronomie.",
            barcelona.destination(),
            ExperienceCategory.CULTURE,
            4
        );
        SeededExperience sevillaExperience = createdOrExistingExperience(
            "Séville au rythme du flamenco",
            "Découvrez Séville à travers le flamenco, les quartiers historiques et la gastronomie andalouse.",
            sevilla.destination(),
            ExperienceCategory.DANCE,
            3
        );
        SeededExperience huescaExperience = createdOrExistingExperience(
            "Aventure dans les Pyrénées aragonaises",
            "Explorez les paysages des Pyrénées aragonaises entre montagnes, villages et espaces naturels.",
            huesca.destination(),
            ExperienceCategory.ADVENTURE,
            4
        );

        createdExperiences += countCreated(bordeauxExperience);
        createdExperiences += countCreated(carcassonneExperience);
        createdExperiences += countCreated(cauteretsExperience);
        createdExperiences += countCreated(barcelonaExperience);
        createdExperiences += countCreated(sevillaExperience);
        createdExperiences += countCreated(huescaExperience);

        createdEvents += createdOrExistingEvent(
            bordeauxExperience,
            "Bordeaux, journée patrimoine et dégustation",
            "Bordeaux, France",
            futureDate(30),
            futureDate(31),
            "Balade dans Bordeaux et dégustation de vins"
        );
        createdEvents += createdOrExistingEvent(
            bordeauxExperience,
            "Saint-Émilion, vignobles et caves",
            "Saint-Émilion, France",
            futureDate(32),
            futureDate(32),
            "Excursion dans les vignobles bordelais"
        );

        createdEvents += createdOrExistingEvent(
            carcassonneExperience,
            "Cité de Carcassonne",
            "Cité de Carcassonne, France",
            futureDate(40),
            futureDate(40),
            "Visite de la cité médiévale"
        );
        createdEvents += createdOrExistingEvent(
            carcassonneExperience,
            "Remparts de Carcassonne",
            "Carcassonne, France",
            futureDate(41),
            futureDate(41),
            "Découverte des remparts et du centre historique"
        );

        createdEvents += createdOrExistingEvent(
            cauteretsExperience,
            "Cauterets, randonnée en montagne",
            "Cauterets, France",
            futureDate(50),
            futureDate(50),
            "Découverte du village et des environs"
        );
        createdEvents += createdOrExistingEvent(
            cauteretsExperience,
            "Pont d'Espagne",
            "Pont d'Espagne, France",
            futureDate(51),
            futureDate(51),
            "Balade dans un site naturel emblématique"
        );

        createdEvents += createdOrExistingEvent(
            barcelonaExperience,
            "Barcelona, architecture et mer",
            "Barcelona, Espagne",
            futureDate(60),
            futureDate(60),
            "Découverte de la ville et de ses quartiers"
        );
        createdEvents += createdOrExistingEvent(
            barcelonaExperience,
            "Barcelona, tapas et soirée libre",
            "Barcelona, Espagne",
            futureDate(61),
            futureDate(61),
            "Soirée gastronomique dans la ville"
        );

        createdEvents += createdOrExistingEvent(
            sevillaExperience,
            "Séville, centre historique",
            "Sevilla, Espagne",
            futureDate(70),
            futureDate(70),
            "Balade dans le centre historique"
        );
        createdEvents += createdOrExistingEvent(
            sevillaExperience,
            "Séville, flamenco en soirée",
            "Sevilla, Espagne",
            futureDate(71),
            futureDate(71),
            "Spectacle flamenco et gastronomie andalouse"
        );

        createdEvents += createdOrExistingEvent(
            huescaExperience,
            "Huesca, montagne et villages",
            "Huesca, Espagne",
            futureDate(80),
            futureDate(80),
            "Découverte de la ville et de ses alentours"
        );
        createdEvents += createdOrExistingEvent(
            huescaExperience,
            "Aínsa, excursion nature",
            "Aínsa, Espagne",
            futureDate(81),
            futureDate(81),
            "Excursion dans les Pyrénées aragonaises"
        );

        logger.info(
            "Dev data seeded:\n- {} destinations created\n- {} experiences created\n- {} events created",
            createdDestinations,
            createdExperiences,
            createdEvents
        );
    }

    private SeededDestination createdOrExistingDestination(String city, String country, String countryCode) {
        return destinationRepository
            .findByCityIgnoreCaseAndCountryCodeIgnoreCase(city, countryCode)
            .map(destination -> new SeededDestination(destination, false))
            .orElseGet(() -> {
                Destination destination = new Destination();
                destination.setCity(city);
                destination.setCountry(country);
                destination.setCountryCode(countryCode);
                return new SeededDestination(destinationRepository.save(destination), true);
            });
    }

    private int countCreated(SeededDestination seededDestination) {
        return seededDestination.created() ? 1 : 0;
    }

    private SeededExperience createdOrExistingExperience(
        String title,
        String description,
        Destination destination,
        ExperienceCategory category,
        Number durationDays
    ) {
        return experienceRepository
            .findByTitleIgnoreCase(title)
            .map(experience -> {
                if (experience.getDestinationEntity() == null) {
                    experience.setDestination(destination);
                    if (experience.getLegacyDestination() == null) {
                        experience.setLegacyDestination(null);
                    }
                    experience.setCategory(category);
                    experience.setDurationDays(durationDays);
                    experienceRepository.save(experience);
                }
                return new SeededExperience(experience, false);
            })
            .orElseGet(() -> {
                Experience experience = new Experience();
                experience.setTitle(title);
                experience.setDescription(description);
                experience.setDestination(destination);
                experience.setLegacyDestination(null);
                experience.setCategory(category);
                experience.setDurationDays(durationDays);
                return new SeededExperience(experienceRepository.save(experience), true);
            });
    }

    private int countCreated(SeededExperience seededExperience) {
        return seededExperience.created() ? 1 : 0;
    }

    private int createdOrExistingEvent(
        SeededExperience seededExperience,
        String name,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        String description
    ) {
        return travelEventRepository
            .findByExperienceIdAndNameIgnoreCase(seededExperience.experience().getId(), name)
            .map(existing -> 0)
            .orElseGet(() -> {
                TravelEvent travelEvent = new TravelEvent();
                travelEvent.setExperience(seededExperience.experience());
                travelEvent.setName(name);
                travelEvent.setLocation(location);
                travelEvent.setStartDate(startDate);
                travelEvent.setEndDate(endDate);
                travelEvent.setDescription(description);
                travelEventRepository.save(travelEvent);
                return 1;
            });
    }

    private LocalDate futureDate(int daysToAdd) {
        return LocalDate.now().plusDays(daysToAdd);
    }

    private record SeededDestination(Destination destination, boolean created) {}

    private record SeededExperience(Experience experience, boolean created) {}
}
