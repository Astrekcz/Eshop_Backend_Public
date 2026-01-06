package org.example.eshopbackend.entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;


@Entity @Table(name="order_items", indexes = {
        @Index(name="ix_items_order", columnList="order_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long oItemId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name="order_id", nullable=false, foreignKey=@ForeignKey(name="fk_item_order"))
    private OrderEntity order;



    @Column(nullable=false, length=64)
    private String nameOfProduct;
    @Column(nullable=false, length=128)
    private String name;
    @Column(nullable=false)
    private int amountOfProducts;
    @Column(nullable=false) private BigDecimal unitPriceCzk;
    @Column(nullable=false) private BigDecimal lineTotalCzk;

    /** Snapshot hmotnosti jednoho kusu v gramech v čase objednávky */
    @Column(name = "weight_grams", nullable = false)
    private Integer weightGrams;
}


