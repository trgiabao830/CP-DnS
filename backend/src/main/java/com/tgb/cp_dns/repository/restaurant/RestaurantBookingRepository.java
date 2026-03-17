package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantBooking;
import com.tgb.cp_dns.enums.BookingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RestaurantBookingRepository extends JpaRepository<RestaurantBooking, Long> {
        Optional<RestaurantBooking> findByVnpTxnRef(String vnpTxnRef);

        Optional<RestaurantBooking> findByAccessToken(String accessToken);

        Page<RestaurantBooking> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        Page<RestaurantBooking> findByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status,
                        Pageable pageable);

        @Query("SELECT b FROM RestaurantBooking b WHERE " +
                        "(:status IS NULL OR b.status = :status) " +
                        "AND (b.bookingTime >= COALESCE(:fromDate, b.bookingTime)) " +
                        "AND (b.bookingTime <= COALESCE(:toDate, b.bookingTime)) " +
                        "AND (:keyword IS NULL OR " +
                        "   (LOWER(b.customerName) LIKE :keyword OR " +
                        "    b.customerPhone LIKE :keyword OR " +
                        "    LOWER(b.customerEmail) LIKE :keyword))")
        Page<RestaurantBooking> findBookingsForAdmin(
                        @Param("status") BookingStatus status,
                        @Param("fromDate") LocalDateTime fromDate,
                        @Param("toDate") LocalDateTime toDate,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        @Query("SELECT b FROM RestaurantBooking b WHERE " +
                        "b.bookingTime >= :startOfDay AND b.bookingTime <= :endOfDay " +
                        "AND b.status != 'CANCELLED' " +
                        "ORDER BY b.bookingTime ASC")
        List<RestaurantBooking> findAllByDateRange(
                        @Param("startOfDay") LocalDateTime startOfDay,
                        @Param("endOfDay") LocalDateTime endOfDay);

        @Query("""
                        SELECT COUNT(b) > 0
                        FROM RestaurantBooking b
                        WHERE b.table.tableId = :tableId
                        AND b.bookingId <> :excludeBookingId
                        AND b.status IN :statuses
                        AND (
                            (b.bookingTime <= :endTime AND b.endTime >= :startTime)
                        )
                        """)
        boolean existsByTableAndDateRangeExcludingBooking(
                        @Param("tableId") Long tableId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime,
                        @Param("excludeBookingId") Long excludeBookingId,
                        @Param("statuses") List<BookingStatus> statuses);

        @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM RestaurantBooking b " +
                        "WHERE b.status = 'COMPLETED' " +
                        "AND b.bookingTime BETWEEN :startTime AND :endTime")
        BigDecimal sumRevenueByTimeRange(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("SELECT COUNT(b) FROM RestaurantBooking b " +
                        "WHERE b.status = 'COMPLETED' " +
                        "AND b.bookingTime BETWEEN :startTime AND :endTime")
        long countCompletedByTimeRange(@Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM RestaurantBooking b WHERE b.status = 'COMPLETED'")
        BigDecimal sumTotalRevenue();

        @Query("SELECT COUNT(b) FROM RestaurantBooking b WHERE b.status = 'COMPLETED'")
        long countTotalCompleted();

        @Query(value = """
                            SELECT
                                TO_CHAR(b.booking_time, 'DD/MM') as label,
                                SUM(b.total_amount) as value
                            FROM restaurant_bookings b
                            WHERE b.status = 'COMPLETED'
                            AND b.booking_time BETWEEN :startTime AND :endTime
                            GROUP BY TO_CHAR(b.booking_time, 'DD/MM')
                            ORDER BY MIN(b.booking_time) ASC
                        """, nativeQuery = true)
        List<Object[]> getDailyRevenueChartData(
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

        @Query(value = """
                            SELECT
                                TO_CHAR(b.booking_time, 'MM/YYYY') as label,
                                SUM(b.total_amount) as value
                            FROM restaurant_bookings b
                            WHERE b.status = 'COMPLETED'
                            AND b.booking_time BETWEEN :startTime AND :endTime
                            GROUP BY TO_CHAR(b.booking_time, 'MM/YYYY')
                            ORDER BY MIN(b.booking_time) ASC
                        """, nativeQuery = true)
        List<Object[]> getMonthlyRevenueChartData(
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);
}
