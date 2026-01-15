package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.mapper.AddressBookMapper;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 新增地址
     * @param addressBookDTO
     */
    @Override
    public void save(AddressBookDTO addressBookDTO) {
        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);
        
        // 设置用户id
        addressBook.setUserId(BaseContext.getCurrentId());
        
        // 如果当前没有地址，则设置为默认地址
        AddressBook query = new AddressBook();
        query.setUserId(addressBook.getUserId());
        List<AddressBook> list = addressBookMapper.list(query);
        if (list == null || list.size() == 0) {
            addressBook.setIsDefault(1);
        } else {
            addressBook.setIsDefault(0);
        }
        
        addressBookMapper.insert(addressBook);
    }

    /**
     * 查询当前登录用户的所有地址
     * @return
     */
    @Override
    public List<AddressBook> list() {
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(userId);
        List<AddressBook> list = addressBookMapper.list(addressBook);
        return list;
    }

    /**
     * 查询默认地址
     * @return
     */
    @Override
    public AddressBook getDefault() {
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = AddressBook.builder()
                .userId(userId)
                .isDefault(1)
                .build();
        
        List<AddressBook> list = addressBookMapper.list(addressBook);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 根据id修改地址
     * @param addressBookDTO
     */
    @Override
    public void update(AddressBookDTO addressBookDTO) {
        AddressBook addressBook = new AddressBook();
        BeanUtils.copyProperties(addressBookDTO, addressBook);
        addressBook.setUserId(BaseContext.getCurrentId());
        
        addressBookMapper.update(addressBook);
    }

    /**
     * 根据id删除地址
     * @param id
     */
    @Override
    public void deleteById(Long id) {
        addressBookMapper.deleteById(id);
    }

    /**
     * 设置默认地址
     * @param addressBookDTO
     */
    @Override
    @Transactional
    public void setDefault(AddressBookDTO addressBookDTO) {
        Long userId = BaseContext.getCurrentId();
        
        // 1. 先将当前用户的所有地址设置为非默认
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(0);
        addressBook.setUserId(userId);
        addressBookMapper.updateIsDefaultByUserId(addressBook);
        
        // 2. 再将当前地址设置为默认地址
        addressBook = new AddressBook();
        addressBook.setId(addressBookDTO.getId());
        addressBook.setIsDefault(1);
        addressBookMapper.update(addressBook);
    }
}