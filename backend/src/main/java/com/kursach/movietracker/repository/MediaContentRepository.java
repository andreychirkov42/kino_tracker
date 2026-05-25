package com.kursach.movietracker.repository;

import com.kursach.movietracker.model.ContentType;
import com.kursach.movietracker.model.MediaContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MediaContentRepository extends JpaRepository<MediaContent, Long> {
    boolean existsByTitleAndReleaseYear(String title, Integer releaseYear);

    @Query("""
        select distinct m
        from MediaContent m
        left join m.genres g
        where (:query is null
            or lower(m.title) like lower(concat('%', :query, '%'))
            or lower(m.originalTitle) like lower(concat('%', :query, '%'))
            or lower(m.director) like lower(concat('%', :query, '%')))
          and (:type is null or m.contentType = :type)
          and (:genre is null or g.name = :genre)
          and (:year is null or m.releaseYear = :year)
          and (:minRating is null or m.averageRating >= :minRating)
        order by m.releaseYear desc, m.title asc
        """)
    List<MediaContent> search(
        @Param("query") String query,
        @Param("type") ContentType type,
        @Param("genre") String genre,
        @Param("year") Integer year,
        @Param("minRating") Double minRating
    );
}
