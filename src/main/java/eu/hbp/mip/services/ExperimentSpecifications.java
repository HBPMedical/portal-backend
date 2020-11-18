package eu.hbp.mip.services;

import eu.hbp.mip.model.DAOs.ExperimentDAO;
import eu.hbp.mip.utils.Exceptions.BadRequestException;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

public class ExperimentSpecifications {
    public static class ExperimentWithName implements Specification<ExperimentDAO> {

        private String name;
        private String regExp;

        public  ExperimentWithName(String name){
            this.name = name;
            this.regExp = name;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb)
        {
            if (name == null) {
                return cb.isTrue(cb.literal(true));
            }
            else {
                regExp = (name.contains("%")?name:name+"%");
            }

            return cb.like( root.get( "name" ), this.regExp );
        }
    }
    public static class ExperimentWithAlgorithm implements Specification<ExperimentDAO> {

        private String algorithm;

        public  ExperimentWithAlgorithm(String algorithm){
            this.algorithm = algorithm;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb)
        {
            if (algorithm == null) {
                return cb.isTrue(cb.literal(true));
            }

            return cb.equal(root.get("algorithm"), this.algorithm);
        }
    }

    public static class ExperimentWithViewed implements Specification<ExperimentDAO> {

        private Boolean viewed;

        public  ExperimentWithViewed(Boolean viewed){
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

        public  ExperimentWithShared(Boolean shared){
            this.shared = shared;
        }

        public Predicate toPredicate(Root<ExperimentDAO> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb) {
            if (shared == null) {
                return cb.isTrue(cb.literal(true));
            }
            return cb.equal(root.get("shared"), this.shared);
        }
    }

    public static class ExperimentOrderBy implements Specification<ExperimentDAO> {

        private String orderBy;
        private Boolean descending ;
        public  ExperimentOrderBy(String orderBy, Boolean descending){
            if (properColumnToBeOrderedBy(orderBy))
                this.orderBy = orderBy;
            else
                throw new BadRequestException("Please provide proper column to order by.");
            if(descending == null)
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

    public static boolean properColumnToBeOrderedBy(String column){
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

