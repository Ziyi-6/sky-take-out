package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        // 1. 对前端传来的明文密码进行MD5加密
        //DigestUtils.md5DigestAsHex是spring框架带的
        password = DigestUtils.md5DigestAsHex(password.getBytes());


        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);
        //第一道门 (getByUsername)：保安查名单，看看有没有“张三”这个人。没这个人？滚。
        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //第二道门 (password.equals)：保安对暗号。暗号对不上？滚。
        //密码比对
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        //第三道门 (getStatus)：保安看黑名单。暗号对了但你在黑名单里？滚。
        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }


    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();

        // 1. 对象属性拷贝：把DTO里的数据（账号、姓名等）拷贝到实体对象employee里
        BeanUtils.copyProperties(employeeDTO, employee);

        // 2. 设置账号状态，默认正常 (1表示正常，0表示锁定)
        // StatusConstant.ENABLE 需要在常量类里确认，通常是 1
        employee.setStatus(StatusConstant.ENABLE);

        // 3. 设置默认密码 123456，并进行 MD5 加密
        // PasswordConstant.DEFAULT_PASSWORD 就是 "123456"
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 4. 设置当前记录的创建时间和修改时间
        //aop帮我们做了

        // 5. 记录创建人id和修改人id
        //aop帮我们做了
        // 6. 调用 Mapper 插入数据库
        employeeMapper.insert(employee); // 这行也会报红，下一步写

    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // 1. 开始分页 (PageHelper 提供的静态方法)
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        // 2. 调用 Mapper 查询 (注意：这里不需要写 limit，PageHelper 会自动加)
        //这个Page类是pagehelper插件带的我们不用自己定义
        //它只拦截紧跟在 PageHelper.startPage() 之后的第一条查询 SQL。
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        // 3. 封装结果 (总记录数 total 和 当前页数据 list)
        long total = page.getTotal();
        List<Employee> records = page.getResult();
        //把查询数据封装成符合前端要求的格式。
        return new PageResult(total, records);
    }

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        // 这里的 build() 是 Lombok 的构建者模式，相当于 new Employee() 然后 setStatus...

        employeeMapper.update(employee); // 复用刚才写的万能 Update
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        // 密码不要传给前端，安全起见设为不可见
        employee.setPassword("****");
        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    public void update(EmployeeDTO employeeDTO) {

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee); // 属性拷贝
        employeeMapper.update(employee); // 再次复用万能 Update！
    }

}
