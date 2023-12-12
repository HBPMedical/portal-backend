package hbp.mip.experiment;

import hbp.mip.utils.Exceptions.BadRequestException;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExperimentSpecifications {
    public static class ExperimentWithName implements Specification<ExperimentDAO> {

        private final String name;
        private String regExp;

        public ExperimentWithName(String name) {
            this.name = name;
            this.regExp = name;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (name == null) {
                return cb.isTrue(cb.literal(true));
            } else {
                regExp = (name.contains("%") ? name : name + "%");
            }

            return cb.like(cb.lower(root.get("name")), this.regExp.toLowerCase());
        }

        @Override
        public @NonNull Specification<ExperimentDAO> and(Specification<ExperimentDAO> other) {
            return Specification.super.and(other);
        }

        @Override
        public @NonNull Specification<ExperimentDAO> or(Specification<ExperimentDAO> other) {
            return Specification.super.or(other);
        }
    }

    public static class ExperimentWithAlgorithm implements Specification<ExperimentDAO> {

        private final String algorithm;

        public ExperimentWithAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (algorithm == null) {
                return cb.isTrue(cb.literal(true));
            }

            return cb.equal(cb.lower(root.get("algorithm")), this.algorithm.toLowerCase());
        }
    }

    public static class ExperimentWithViewed implements Specification<ExperimentDAO> {

        private final Boolean viewed;

        public ExperimentWithViewed(Boolean viewed) {
            this.viewed = viewed;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder cb) {
            if (viewed == null) {
                return cb.isTrue(cb.literal(true)); // always true = no filtering
            }
            return cb.equal(root.get("viewed"), this.viewed);
        }
    }

    public static class ExperimentWithShared implements Specification<ExperimentDAO> {

        private final Boolean shared;

        public ExperimentWithShared(Boolean shared) {
            this.shared = shared;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (shared == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("shared"), this.shared);
        }
    }

    public static class MyExperiment implements Specification<ExperimentDAO> {

        private final String username;

        public MyExperiment(String username) {
            this.username = username;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (username == null) {
                return cb.isTrue(cb.literal(true));
            }
            Join<Object, Object> experimentDAOUserDAOJoin = root.join("createdBy");
            return cb.equal(experimentDAOUserDAOJoin.get("username"), username);
        }
    }

    public static class SharedExperiment implements Specification<ExperimentDAO> {

        private final boolean shared;

        public SharedExperiment(boolean shared) {
            this.shared = shared;
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (!shared) {
                return cb.isTrue(cb.literal(false));
            }
            return cb.equal(root.get("shared"), true);
        }
    }

    public static class ExperimentOrderBy implements Specification<ExperimentDAO> {

        private final String orderBy;
        private final Boolean descending;

        public ExperimentOrderBy(String orderBy, Boolean descending) {
            if (properColumnToBeOrderedBy(orderBy))
                this.orderBy = orderBy;
            else
                throw new BadRequestException("Please provide proper column to order by.");
            this.descending = Objects.requireNonNullElse(descending, true);
        }

        public Predicate toPredicate(@NonNull Root<ExperimentDAO> root, @NonNull CriteriaQuery<?> criteriaQuery, @NonNull CriteriaBuilder cb) {
            if (descending) {
                criteriaQuery.orderBy(cb.desc(root.get(orderBy)));
            } else {
                criteriaQuery.orderBy(cb.asc(root.get(orderBy)));
            }
            return cb.isTrue(cb.literal(true));
        }

    }

    public static boolean properColumnToBeOrderedBy(String column) {
        {
            List<String> properColumns = new ArrayList<>();
            properColumns.add("uuid");
            properColumns.add("name");
            properColumns.add("created_by_username");
            properColumns.add("algorithm");
            properColumns.add("created");
            properColumns.add("status");
            properColumns.add("shared");
            properColumns.add("viewed");
            return properColumns.contains(column);
        }
    }
}

