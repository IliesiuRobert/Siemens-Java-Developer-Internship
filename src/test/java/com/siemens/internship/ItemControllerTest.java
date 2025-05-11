package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.controller.ItemController;
import com.siemens.internship.exception.ResourceNotFoundException;
import com.siemens.internship.model.Item;
import com.siemens.internship.service.ItemService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mvc;
    @MockBean
    private ItemService itemService;
    @Autowired private ObjectMapper om;

    @Test
    void create_valid() throws Exception {
        Item in = new Item(null,"name1","description1","NEW","ex1@gmail.com");
        Item out = new Item(5L,"name2","description2","NEW","ex2@gmail.com");
        when(itemService.save(any())).thenReturn(out);

        mvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(in)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getAll() throws Exception {
        when(itemService.findAll()).thenReturn(List.of(new Item(1L,"name3","description3","NEW","ex3@gmail.com")));
        mvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getItemById_found() throws Exception {
        Item it = new Item(7L,"foo","bar","NEW","foo@bar.com");
        when(itemService.findByIdOrThrow(7L)).thenReturn(it);

        mvc.perform(get("/api/items/{id}", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("foo"));
    }

    @Test
    void getItemById_notFound() throws Exception {
        when(itemService.findByIdOrThrow(99L))
                .thenThrow(new ResourceNotFoundException("Item 99 not found"));

        mvc.perform(get("/api/items/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateItem_found() throws Exception {
        Item in  = new Item(null,"upd","desc","NEW","u@d.com");
        Item out = new Item(3L,"upd","desc","NEW","u@d.com");
        when(itemService.update(eq(3L), any())).thenReturn(out);

        mvc.perform(put("/api/items/{id}", 3L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(in)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("upd"));
    }

    @Test
    void updateItem_notFound() throws Exception {
        Item in = new Item(null,"x","y","NEW","x@y.com");
        when(itemService.update(eq(42L), any()))
                .thenThrow(new ResourceNotFoundException("Item 42 not found"));

        mvc.perform(put("/api/items/{id}", 42L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(in)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_found() throws Exception {
        doNothing().when(itemService).delete(5L);

        mvc.perform(delete("/api/items/{id}", 5L))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Item 8 not found"))
                .when(itemService).delete(8L);

        mvc.perform(delete("/api/items/{id}", 8L))
                .andExpect(status().isNotFound());
    }

    @Test
    void processItems_async() throws Exception {
        List<Item> processed = List.of(
                new Item(1L,"a","d","PROCESSED","a@b.com"),
                new Item(2L,"b","d","PROCESSED","b@c.com")
        );
        when(itemService.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(processed));

        MvcResult mvcResult = mvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }
}

