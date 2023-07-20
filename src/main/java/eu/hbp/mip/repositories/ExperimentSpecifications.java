package eu.hbp.mip.repositories;

import eu.hbp.mip.models.DAOs.ExperimentDAO;
import eu.hbp.mip.models.DAOs.UserDAO;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

public class ExperimentSpecifications {
    public static class ExperimentWithName implements Specification<ExperimentDAO> {

        private String name;
        private String regExp;

        public ExperimentWithName(String name) {
            this.name = name;
            this.regExp = name;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (name == null) {
                return cb.isTrue(cb.literal(true));
            } else {
                regExp = (name.contains("%") ? name : name + "%");
            }

            return cb.like(cb.lower(root.get("name")), this.regExp.toLowerCase());
        }
    }

    public static class ExperimentWithAlgorithm implements Specification<ExperimentDAO> {

        private String algorithm;

        public ExperimentWithAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (algorithm == null) {
                return cb.isTrue(cb.literal(true));
            }

            return cb.equal(cb.lower(root.get("algorithm")), this.algorithm.toLowerCase());
        }
    }

    public static class ExperimentWithViewed implements Specification<ExperimentDAO> {

        private Boolean viewed;

        public ExperimentWithViewed(Boolean viewed) {
            this.viewed = viewed;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
            if (viewed == null) {
                return cb.isTrue(cb.literal(true)); // always true = no filtering
            }
            return cb.equal(root.get("viewed"), this.viewed);
        }
    }

    public static class ExperimentWithShared implements Specification<ExperimentDAO> {

        private Boolean shared;

        public ExperimentWithShared(Boolean shared) {
            this.shared = shared;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (shared == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("shared"), this.shared);
        }
    }

    public static class MyExperiment implements Specification<ExperimentDAO> {

        private String username;

        public MyExperiment(String username) {
            this.username = username;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (username == null) {
                return cb.isTrue(cb.literal(true));
            }
            Join<ExperimentDAO, UserDAO> experimentDAOUserDAOJoin = root.join("createdBy");
            return cb.equal(experimentDAOUserDAOJoin.get("username"), username);
        }
    }

    public static class SharedExperiment implements Specification<ExperimentDAO> {

        private boolean shared;

        public SharedExperiment(boolean shared) {
            this.shared = shared;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (shared == false) {
                return cb.isTrue(cb.literal(false));
            }
            return cb.equal(root.get("shared"), shared);
        }
    }

    public static class ExperimentOrderBy implements Specification<ExperimentDAO> {

        private String orderBy;
        private Boolean descending;

        public ExperimentOrderBy(String orderBy, Boolean descending) {
            if (properColumnToBeOrderedBy(orderBy))
                this.orderBy = orderBy;
            else
                throw new BadRequestException("Please provide proper column to order by.");
            if (descending == null)
                this.descending = true;
            else
                this.descending = descending;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
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

