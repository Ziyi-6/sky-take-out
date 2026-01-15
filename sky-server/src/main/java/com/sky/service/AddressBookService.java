package com.sky.service;

import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    /**
     * 新增地址
     * @param addressBookDTO
     */
    void save(AddressBookDTO addressBookDTO);

    /**
     * 查询当前登录用户的所有地址
     * @return
     */
    List<AddressBook> list();

    /**
     * 查询默认地址
     * @return
     */
    AddressBook getDefault();

    /**
     * 根据id修改地址
     * @param addressBookDTO
     */
    void update(AddressBookDTO addressBookDTO);

    /**
     * 根据id删除地址
     * @param id
     */
    void deleteById(Long id);

    /**
     * 设置默认地址
     * @param addressBookDTO
     */
    void setDefault(AddressBookDTO addressBookDTO);
}