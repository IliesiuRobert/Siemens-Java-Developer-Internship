package com.siemens.internship;

import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class ItemServiceTest {
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findByIdOrThrowTest_found() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(new Item(1L, "name1", "description1", "NEW", "ex1@gmail.com")));
        Item item = itemService.findByIdOrThrow(1L);
        assertEquals("name1", item.getName());
    }

    @Test
    void findByIdOrThrowTest_notFound() {
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> itemService.findByIdOrThrow(2L).getClass());
    }

    @Test
    void processItemsAsync_all() throws Exception {
        when(itemRepository.findAllIds()).thenReturn(List.of(10L, 20L));
        when(itemRepository.findById(10L)).thenReturn(Optional.of(new Item(10L,"name2","description2",null,"ex2@gmail.com")));
        when(itemRepository.findById(20L)).thenReturn(Optional.of(new Item(20L,"name3","description3",null,"ex3@gmail.com")));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> processed = future.get();
        assertEquals(2, processed.size());
        processed.forEach(it -> assertEquals("PROCESSED", it.getStatus()));
    }
}
