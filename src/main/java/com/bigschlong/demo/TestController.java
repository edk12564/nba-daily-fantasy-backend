package com.bigschlong.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("books-rest")
public class TestController {

    @GetMapping(value = "/{id}", produces = "application/json")
    public Integer getBook(@PathVariable int id) {
        return findBookById(id);
    }

    private Integer findBookById(int id) {
        int[] books = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int book : books) {
            System.out.println(book);
        }
        return id;
    }
}
