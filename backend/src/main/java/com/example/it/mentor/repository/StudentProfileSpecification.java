package com.example.it.mentor.repository;

import com.example.it.mentor.dto.student.StudentSearchFilter;
import com.example.it.mentor.entity.StudentProfile;
import com.example.it.mentor.entity.StudentSkill;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class StudentProfileSpecification {

    private StudentProfileSpecification() {}

    public static Specification<StudentProfile> build(StudentSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + escapeLike(filter.q().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(cb.coalesce(root.get("desiredPosition"), "")), pattern)
                ));
            }

            if (filter.cityId() != null) {
                predicates.add(cb.equal(root.get("city").get("id"), filter.cityId()));
            }

            if (filter.skillIds() != null && !filter.skillIds().isEmpty()) {
                List<Long> skillIds = filter.skillIds();
                Subquery<Long> sub = query.subquery(Long.class);
                Root<StudentSkill> skillRoot = sub.from(StudentSkill.class);
                sub.select(cb.count(skillRoot.get("skill").get("id")))
                        .where(
                                cb.equal(skillRoot.get("studentProfile").get("id"), root.get("id")),
                                skillRoot.get("skill").get("id").in(skillIds)
                        );
                predicates.add(cb.greaterThanOrEqualTo(sub, (long) skillIds.size()));
            }

            if (filter.employmentType() != null) {
                predicates.add(cb.isMember(filter.employmentType(), root.get("employmentTypes")));
            }

            if (filter.workFormat() != null) {
                predicates.add(cb.isMember(filter.workFormat(), root.get("workFormats")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
