package app.owlcms.relay.backend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import app.owlcms.relay.ui.encoders.LocalDateToStringEncoder;

/**
 * Simple backend service to store and retrieve {@link Review} instances.
 */
public class ReviewService {

    /**
     * Helper class to initialize the singleton Service in a thread-safe way and
     * to keep the initialization ordering clear between the two services. See
     * also: https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
     */
    private static class SingletonHolder {
        static final ReviewService INSTANCE = createDemoReviewService();

        /** This class is not meant to be instantiated. */
        private SingletonHolder() {
        }

        private static ReviewService createDemoReviewService() {
            final ReviewService reviewService = new ReviewService();
            Random r = new Random();
            int reviewCount = 20 + r.nextInt(30);
            List<Map.Entry<String, String>> beverages = new ArrayList<>(
                    StaticData.BEVERAGES.entrySet());

            for (int i = 0; i < reviewCount; i++) {
                Review review = new Review();
                Map.Entry<String, String> beverage = beverages
                        .get(r.nextInt(StaticData.BEVERAGES.size()));
                Category category = CategoryService.getInstance()
                        .findCategoryOrThrow(beverage.getValue());

                review.setName(beverage.getKey());
                LocalDate testDay = getRandomDate();
                review.setDate(testDay);
                review.setScore(1 + r.nextInt(5));
                review.setCategory(category);
                review.setCount(1 + r.nextInt(15));
                reviewService.saveReview(review);
            }

            return reviewService;
        }

        private static LocalDate getRandomDate() {
            long minDay = LocalDate.of(1930, 1, 1).toEpochDay();
            long maxDay = LocalDate.now().toEpochDay();
            long randomDay = ThreadLocalRandom.current().nextLong(minDay,
                    maxDay);
            return LocalDate.ofEpochDay(randomDay);
        }
    }

    private Map<Long, Review> reviews = new HashMap<>();
    private AtomicLong nextId = new AtomicLong(0);

    /**
     * Declared private to ensure uniqueness of this Singleton.
     */
    private ReviewService() {
    }

    /**
     * Gets the unique instance of this Singleton.
     *
     * @return the unique instance of this Singleton
     */
    public static ReviewService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Fetches the reviews matching the given filter text.
     *
     * The matching is case insensitive. When passed an empty filter text, the
     * method returns all categories. The returned list is ordered by name.
     *
     * @param filter
     *            the filter text
     * @return the list of matching reviews
     */
    public List<Review> findReviews(String filter) {
        String normalizedFilter = filter.toLowerCase();

        return reviews.values().stream().filter(
                review -> filterTextOf(review).contains(normalizedFilter))
                .sorted((r1, r2) -> r2.getId().compareTo(r1.getId()))
                .collect(Collectors.toList());
    }

    private String filterTextOf(Review review) {
        LocalDateToStringEncoder dateConverter = new LocalDateToStringEncoder();
        // Use a delimiter which can't be entered in the search box,
        // to avoid false positives
        String filterableText = Stream
                .of(review.getName(),
                        review.getCategory() == null ? StaticData.UNDEFINED
                                : review.getCategory().getName(),
                        String.valueOf(review.getScore()),
                        String.valueOf(review.getCount()),
                        dateConverter.encode(review.getDate()))
                .collect(Collectors.joining("\t"));
        return filterableText.toLowerCase();
    }

    /**
     * Deletes the given review from the review store.
     *
     * @param review
     *            the review to delete
     * @return true if the operation was successful, otherwise false
     */
    public boolean deleteReview(Review review) {
        return reviews.remove(review.getId()) != null;
    }

    /**
     * Persists the given review into the review store.
     *
     * If the review is already persistent, the saved review will get updated
     * with the field values of the given review object. If the review is new
     * (i.e. its id is null), it will get a new unique id before being saved.
     *
     * @param dto
     *            the review to save
     */
    public void saveReview(Review dto) {
        Review entity = reviews.get(dto.getId());
        Category category = dto.getCategory();

        if (category != null) {
            // The case when the category is new (not persisted yet, thus
            // has null id) is not handled here, because it can't currently
            // occur via the UI.
            // Note that Category.UNDEFINED also gets mapped to null.
            category = CategoryService.getInstance()
                    .findCategoryById(category.getId()).orElse(null);
        }
        if (entity == null) {
            // Make a copy to keep entities and DTOs separated
            entity = new Review(dto);
            if (dto.getId() == null) {
                entity.setId(nextId.incrementAndGet());
            }
            reviews.put(entity.getId(), entity);
        } else {
            entity.setScore(dto.getScore());
            entity.setName(dto.getName());
            entity.setDate(dto.getDate());
            entity.setCount(dto.getCount());
        }
        entity.setCategory(category);
    }
}
