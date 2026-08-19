package metropolitan.ac.rs.coworking.repository;

import metropolitan.ac.rs.coworking.model.Booking;
import metropolitan.ac.rs.coworking.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByWorkspaceId(Long workspaceId);
    List<Booking> findByStatus(BookingStatus status);

    @Query("""
            SELECT b FROM Booking b
            WHERE b.workspace.id = :workspaceId
            AND b.status = :status
            AND (:startTime < b.endTime AND :endTime > b.startTime)
            """)
    List<Booking> findOverlappingApprovedBookings(
            @Param("workspaceId") Long workspaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status")  BookingStatus status
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.workspace.id = :workspaceId
            AND b.status = :status
            AND b.id != :currentBookingId
            AND (:startTime < b.endTime AND :endTime > b.startTime)
            """)
    List<Booking> findOverlappingPendingBookings(
            @Param("workspaceId") Long workspaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("currentBookingId") Long currentBookingId,
            @Param("status") BookingStatus status
    );
}
