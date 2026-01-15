package com.sky.controller.user;

import com.sky.dto.AddressBookDTO;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("userAddressBookController")
@RequestMapping("/user/addressBook")
@Api(tags = "小程序端-地址簿接口")
@Slf4j
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     * @param addressBookDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增地址")
    public Result<String> save(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("新增地址：{}", addressBookDTO);
        addressBookService.save(addressBookDTO);
        return Result.success();
    }

    /**
     * 查询当前登录用户的所有地址
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询当前用户所有地址")
    public Result<List<AddressBook>> list() {
        log.info("查询当前用户所有地址");
        List<AddressBook> list = addressBookService.list();
        return Result.success(list);
    }

    /**
     * 查询默认地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation("查询默认地址")
    public Result<AddressBook> getDefault() {
        log.info("查询默认地址");
        AddressBook addressBook = addressBookService.getDefault();
        return Result.success(addressBook);
    }

    /**
     * 根据id修改地址
     * @param addressBookDTO
     * @return
     */
    @PutMapping
    @ApiOperation("根据id修改地址")
    public Result<String> update(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("根据id修改地址：{}", addressBookDTO);
        addressBookService.update(addressBookDTO);
        return Result.success();
    }

    /**
     * 根据id删除地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("根据id删除地址")
    public Result<String> deleteById(Long id) {
        log.info("根据id删除地址：{}", id);
        addressBookService.deleteById(id);
        return Result.success();
    }

    /**
     * 设置默认地址
     * @param addressBookDTO
     * @return
     */
    @PutMapping("/default")
    @ApiOperation("设置默认地址")
    public Result<String> setDefault(@RequestBody AddressBookDTO addressBookDTO) {
        log.info("设置默认地址：{}", addressBookDTO);
        addressBookService.setDefault(addressBookDTO);
        return Result.success();
    }
}