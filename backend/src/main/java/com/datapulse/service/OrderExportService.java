package com.datapulse.service;

import com.datapulse.model.Order;
import com.datapulse.model.OrderItem;
import com.datapulse.model.Product;
import com.datapulse.repository.OrderItemRepository;
import com.datapulse.repository.OrderRepository;
import com.datapulse.repository.ProductRepository;
import com.datapulse.security.UserDetailsImpl;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public byte[] exportCsv(Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        List<Order> orders = orderRepository.findByUserId(user.getId());
        List<String> orderIds = orders.stream().map(Order::getId).toList();
        List<OrderItem> items = orderItemRepository.findByOrderIdIn(orderIds);
        Map<String, List<OrderItem>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        List<String> productIds = items.stream().map(OrderItem::getProductId).distinct().toList();
        Map<String, Product> productMap = productRepository.findAllById(productIds)
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            writer.writeNext(new String[]{
                    "Order ID", "Date", "Status", "Product", "Quantity", "Price", "Subtotal", "Tax", "Grand Total"
            });

            for (Order order : orders) {
                List<OrderItem> orderItems = itemsByOrder.getOrDefault(order.getId(), List.of());
                for (OrderItem item : orderItems) {
                    Product p = productMap.get(item.getProductId());
                    writer.writeNext(new String[]{
                            order.getId(),
                            order.getCreatedAt() != null ? order.getCreatedAt().format(FMT) : "",
                            order.getStatus() != null ? order.getStatus().getTrLabel() : "",
                            p != null ? p.getName() : item.getProductId(),
                            String.valueOf(item.getQuantity()),
                            String.format("%.2f", item.getPrice()),
                            String.format("%.2f", order.getSubtotal()),
                            String.format("%.2f", order.getTaxAmount()),
                            String.format("%.2f", order.getGrandTotal())
                    });
                }
            }
            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export CSV: " + e.getMessage(), e);
        }
    }

    public byte[] exportPdf(Authentication auth) {
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        List<Order> orders = orderRepository.findByUserId(user.getId());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            com.itextpdf.kernel.pdf.PdfWriter pdfWriter = new com.itextpdf.kernel.pdf.PdfWriter(baos);
            com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter);
            com.itextpdf.layout.Document doc = new com.itextpdf.layout.Document(pdfDoc);

            doc.add(new com.itextpdf.layout.element.Paragraph("DataPulse - Order History")
                    .setFontSize(18).setBold());
            doc.add(new com.itextpdf.layout.element.Paragraph("User: " + user.getUsername())
                    .setFontSize(10));
            doc.add(new com.itextpdf.layout.element.Paragraph(" "));

            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(5);
            table.addHeaderCell("Order ID");
            table.addHeaderCell("Date");
            table.addHeaderCell("Status");
            table.addHeaderCell("Grand Total");
            table.addHeaderCell("Payment");

            for (Order order : orders) {
                table.addCell(order.getId());
                table.addCell(order.getCreatedAt() != null ? order.getCreatedAt().format(FMT) : "");
                table.addCell(order.getStatus() != null ? order.getStatus().getTrLabel() : "");
                table.addCell(String.format("%.2f", order.getGrandTotal()));
                table.addCell(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "");
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PDF: " + e.getMessage(), e);
        }
    }
}
