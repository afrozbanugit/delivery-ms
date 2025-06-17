package com.delivery.ms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.delivery.ms.dto.CustomerOrder;
import com.delivery.ms.dto.DeliveryEvent;
import com.delivery.ms.entity.Delivery;
import com.delivery.ms.entity.DeliveryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/api")
public class DeliveryController {

	@Autowired
	private DeliveryRepository repository;

	@Autowired
	private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;

	@GetMapping("/health")
	public String healthStatus(){
		return "Delivery MS Health ok!";
	}

	@KafkaListener(topics = "new-stock", groupId = "stock-group")
	public void deliverOrder(String event) throws JsonMappingException, JsonProcessingException {
		System.out.println("Inside ship order for order "+event);
		
		Delivery shipment = new Delivery();
		DeliveryEvent inventoryEvent = new ObjectMapper().readValue(event, DeliveryEvent.class);
		CustomerOrder order = inventoryEvent.getOrder();

		try {
			if (order.getAddress() == null) {
				throw new Exception("Address not present");
			}

			shipment.setAddress(order.getAddress());
			shipment.setOrderId(order.getOrderId());

			shipment.setStatus("success");

			repository.save(shipment);
		} catch (Exception e) {
			shipment.setOrderId(order.getOrderId());
			shipment.setStatus("failed");
			repository.save(shipment);

			System.out.println(order);

			DeliveryEvent reverseEvent = new DeliveryEvent();
			reverseEvent.setType("STOCK_REVERSED");
			reverseEvent.setOrder(order);
			kafkaTemplate.send("reversed-stock", reverseEvent);
		}
	}
}
