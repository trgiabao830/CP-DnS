package com.tgb.cp_dns.repository.homestay;

import com.tgb.cp_dns.entity.homestay.HomestayBooking;
import com.tgb.cp_dns.enums.BookingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HomestayBookingRepository extends JpaRepository<HomestayBooking, Long> {

        Optional<HomestayBooking> findByVnpTxnRef(String vnpTxnRef);

        Optional<HomestayBooking> findByAccessToken(String accessToken);

        Page<HomestayBooking> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

        Page<HomestayBooking> findByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, BookingStatus status,
                        Pageable pageable);

        @Query("SELECT b.room.roomId FROM HomestayBooking b " +
                        "WHERE b.status NOT IN ('CANCELLED', 'NO_SHOW') " +
                        "AND b.checkInDate < :checkOutDate " +
                        "AND b.checkOutDate > :checkInDate")
        List<Long> findOccupiedRoomIds(@Param("checkInDate") LocalDate checkInDate,
                        @Param("checkOutDate") LocalDate checkOutDate);

        @Query("SELECT b FROM HomestayBooking b WHERE " +
                        "(:status IS NULL OR b.status = :status) " +
                        "AND (b.checkInDate >= COALESCE(:fromDate, b.checkInDate)) " +
                        "AND (b.checkInDate <= COALESCE(:toDate, b.checkInDate)) " +
                        "AND (:keyword IS NULL OR " +
                        "   (LOWER(b.customerName) LIKE :keyword OR " +
                        "    b.customerPhone LIKE :keyword OR " +
                        "    LOWER(b.customerEmail) LIKE :keyword))")
        Page<HomestayBooking> findBookingsForAdmin(
                        @Param("status") BookingStatus status,
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        List<HomestayBooking> findAllByCheckInDateAndStatusNot(LocalDate date, BookingStatus status);

        List<HomestayBooking> findAllByCheckOutDateAndStatusNot(LocalDate date, BookingStatus status);

        List<HomestayBooking> findAllByStatus(BookingStatus status);

        @Query("SELECT b.room.roomId FROM HomestayBooking b " +
                        "WHERE b.status NOT IN ('CANCELLED', 'NO_SHOW', 'REJECTED') " +
                        "AND b.checkInDate < :checkOutDate " +
                        "AND b.checkOutDate > :checkInDate " +
                        "AND b.bookingId <> :excludeBookingId")
        List<Long> findOccupiedRoomIdsExcludingBooking(
                        @Param("checkInDate") LocalDate checkInDate,
                        @Param("checkOutDate") LocalDate checkOutDate,
                        @Param("excludeBookingId") Long excludeBookingId);

        @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM HomestayBooking b " +
                        "WHERE b.status = 'COMPLETED' " +
                        "AND b.checkInDate BETWEEN :startDate AND :endDate")
        BigDecimal sumRevenueByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT COUNT(b) FROM HomestayBooking b " +
                        "WHERE b.status = 'COMPLETED' " +
                        "AND b.checkInDate BETWEEN :startDate AND :endDate")
        long countCompletedByDateRange(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM HomestayBooking b WHERE b.status = 'COMPLETED'")
        BigDecimal sumTotalRevenue();

        @Query("SELECT COUNT(b) FROM HomestayBooking b WHERE b.status = 'COMPLETED'")
        long countTotalCompleted();

        @Query(value = """
                        SELECT
                            TO_CHAR(b.check_in_date, 'DD/MM') as label,
                            SUM(b.total_amount) as value
                        FROM homestay_bookings b
                        WHERE b.status = 'COMPLETED'
                        AND b.check_in_date BETWEEN :startDate AND :endDate
                        GROUP BY TO_CHAR(b.check_in_date, 'DD/MM')
                        ORDER BY MIN(b.check_in_date) ASC
                        """, nativeQuery = true)
        List<Object[]> getDailyRevenueChartData(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query(value = """
                        SELECT
                            TO_CHAR(b.check_in_date, 'MM/YYYY') as label,
                            SUM(b.total_amount) as value
                        FROM homestay_bookings b
                        WHERE b.status = 'COMPLETED'
                        AND b.check_in_date BETWEEN :startDate AND :endDate
                        GROUP BY TO_CHAR(b.check_in_date, 'MM/YYYY')
                        ORDER BY MIN(b.check_in_date) ASC
                        """, nativeQuery = true)
        List<Object[]> getMonthlyRevenueChartData(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}