package com.siemens.internship.service;

import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);

    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Item findByIdOrThrow(Long id) {
        return itemRepository.findById(id).
                orElseThrow(() -> new ResourceNotFoundException("Item with id " + id + " not found"));
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public Item update(Long id, Item item) {
        Item existingItem = findByIdOrThrow(id);
        existingItem.setName(item.getName());
        existingItem.setDescription(item.getDescription());
        existingItem.setEmail(item.getEmail());

        return itemRepository.save(existingItem);
    }

    public void delete(Long id) {
        findByIdOrThrow(id);
        itemRepository.deleteById(id);
    }

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    Item item = itemRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Item with id " + id + " not found"));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}

                    item.setStatus("PROCESSED");
                    return itemRepository.save(item);
                }, executor)).collect(Collectors.toList());

        CompletableFuture<Void> allDone = CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]));

        return allDone.thenApply(v ->
            futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList())
        );
    }

}

