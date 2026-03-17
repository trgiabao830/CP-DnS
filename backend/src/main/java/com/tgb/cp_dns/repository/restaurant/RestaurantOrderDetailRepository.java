package com.tgb.cp_dns.repository.restaurant;

import com.tgb.cp_dns.entity.restaurant.RestaurantOrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantOrderDetailRepository extends JpaRepository<RestaurantOrderDetail, Long> {
}
