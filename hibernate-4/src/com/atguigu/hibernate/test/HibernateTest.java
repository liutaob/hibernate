package com.atguigu.hibernate.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.atguigu.hibernate.dao.DepartmentDao;
import com.atguigu.hibernate.entities.Department;
import com.atguigu.hibernate.entities.Employee;
import com.atguigu.hibernate.hibernate.HibernateUtils;

public class HibernateTest {

	private SessionFactory sessionFactory;
	private Session session;
	private Transaction transaction;
	
	@Before
	public void init(){
		Configuration configuration = new Configuration().configure();
		ServiceRegistry serviceRegistry = 
				new ServiceRegistryBuilder().applySettings(configuration.getProperties())
				                            .buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
	}
	
	@After
	public void destroy(){
		transaction.commit();
		session.close();
		sessionFactory.close();
	}
	
	@Test
	public void testBatch(){
		session.doWork(new Work() {			
			@Override
			public void execute(Connection connection) throws SQLException {
				//通过 JDBC 原生的 API 进行操作, 效率最高, 速度最快!
			}
		});
	}
	
	@Test
	public void testManageSession(){
		
		//获取 Session
		//开启事务
		Session session = HibernateUtils.getInstance().getSession();
		System.out.println("-->" + session.hashCode());
		Transaction transaction = session.beginTransaction();
		
		DepartmentDao departmentDao = new DepartmentDao();
		
		Department dept = new Department();
		dept.setName("ATGUIGU");
		
		departmentDao.save(dept);
		departmentDao.save(dept);
		departmentDao.save(dept);
		
		//若 Session 是由 thread 来管理的, 则在提交或回滚事务时, 已经关闭 Session 了. 
		transaction.commit();
		System.out.println(session.isOpen()); 
	}
	
	@Test
	public void testQueryIterate(){
		Department dept = (Department) session.get(Department.class, 70);
		System.out.println(dept.getName());
		System.out.println(dept.getEmps().size()); 
		
		Query query = session.createQuery("FROM Employee e WHERE e.dept.id = 80");
//		List<Employee> emps = query.list();
//		System.out.println(emps.size()); 
		
		Iterator<Employee> empIt = query.iterate();
		while(empIt.hasNext()){
			System.out.println(empIt.next().getName()); 
		}
	}
	
	@Test
	public void testUpdateTimeStampCache(){
		Query query = session.createQuery("FROM Employee");
		query.setCacheable(true);
		
		List<Employee> emps = query.list();
		System.out.println(emps.size());
		
		Employee employee = (Employee) session.get(Employee.class, 100);
		employee.setSalary(30000);
		
		emps = query.list();
		System.out.println(emps.size());
	}
	
	@Test
	public void testQueryCache(){
		Query query = session.createQuery("FROM Employee");
		query.setCacheable(true);
		
		List<Employee> emps = query.list();
		System.out.println(emps.size());
		
		emps = query.list();
		System.out.println(emps.size());
		
		Criteria criteria = session.createCriteria(Employee.class);
		criteria.setCacheable(true);
	}
	
	@Test
	public void testCollectionSecondLevelCache(){
		Department dept = (Department) session.get(Department.class, 80);
		System.out.println(dept.getName());
		System.out.println(dept.getEmps().size()); 
		
		transaction.commit();
		session.close();
		
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
		
		Department dept2 = (Department) session.get(Department.class, 80);
		System.out.println(dept2.getName());
		System.out.println(dept2.getEmps().size()); 
	}
	
	@Test
	public void testHibernateSecondLevelCache(){
		Employee employee = (Employee) session.get(Employee.class, 100);
		System.out.println(employee.getName()); 
		
		transaction.commit();
		session.close();
		
		session = sessionFactory.openSession();
		transaction = session.beginTransaction();
		
		Employee employee2 = (Employee) session.get(Employee.class, 100);
		System.out.println(employee2.getName()); 
	}
	
	@Test
	public void testHQLUpdate(){
		String hql = "DELETE FROM Department d WHERE d.id = :id";
		
		session.createQuery(hql).setInteger("id", 280)
		                        .executeUpdate();
	}
	
	@Test
	public void testNativeSQL(){
		String sql = "INSERT INTO gg_department VALUES(?, ?)";
		Query query = session.createSQLQuery(sql);
		
		query.setInteger(0, 280)
		     .setString(1, "ATGUIGU")
		     .executeUpdate();
	}
	
	@Test
	public void testQBC4(){
		Criteria criteria = session.createCriteria(Employee.class);
		
		//1. 添加排序
		criteria.addOrder(Order.asc("salary"));
		criteria.addOrder(Order.desc("email"));
		
		//2. 添加翻页方法
		int pageSize = 5;
		int pageNo = 3;
		criteria.setFirstResult((pageNo - 1) * pageSize)
		        .setMaxResults(pageSize)
		        .list();
	}
	
	@Test
	public void testQBC3(){
		Criteria criteria = session.createCriteria(Employee.class);
		
		//统计查询: 使用 Projection 来表示: 可以由 Projections 的静态方法得到
		criteria.setProjection(Projections.max("salary"));
		
		System.out.println(criteria.uniqueResult()); 
	}
	
	@Test
	public void testQBC2(){
		Criteria criteria = session.createCriteria(Employee.class);
		
		//1. AND: 使用 Conjunction 表示
		//Conjunction 本身就是一个 Criterion 对象
		//且其中还可以添加 Criterion 对象
		Conjunction conjunction = Restrictions.conjunction();
		conjunction.add(Restrictions.like("name", "a", MatchMode.ANYWHERE));
		Department dept = new Department();
		dept.setId(80);
		conjunction.add(Restrictions.eq("dept", dept));
		System.out.println(conjunction); 
		
		//2. OR
		Disjunction disjunction = Restrictions.disjunction();
		disjunction.add(Restrictions.ge("salary", 6000F));
		disjunction.add(Restrictions.isNull("email"));
		
		criteria.add(disjunction);
		criteria.add(conjunction);
		
		criteria.list();
	}
	
	@Test
	public void testQBC(){
		//1. 创建一个 Criteria 对象
		Criteria criteria = session.createCriteria(Employee.class);
		
		//2. 添加查询条件: 在 QBC 中查询条件使用 Criterion 来表示
		//Criterion 可以通过 Restrictions 的静态方法得到
		criteria.add(Restrictions.eq("email", "SKUMAR"));
		criteria.add(Restrictions.gt("salary", 5000F));
		
		//3. 执行查询
		Employee employee = (Employee) criteria.uniqueResult();
		System.out.println(employee); 
	}
	
	@Test
	public void testLeftJoinFetch2(){
		String hql = "SELECT e FROM Employee e INNER JOIN e.dept";
		Query query = session.createQuery(hql);
		
		List<Employee> emps = query.list();
		System.out.println(emps.size()); 
		
		for(Employee emp: emps){
			System.out.println(emp.getName() + ", " + emp.getDept().getName());
		}
	}
	
	@Test
	public void testLeftJoin(){
		String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN d.emps";
		Query query = session.createQuery(hql);
		
		List<Department> depts = query.list();
		System.out.println(depts.size());
		
		for(Department dept: depts){
			System.out.println(dept.getName() + ", " + dept.getEmps().size()); 
		}
		
//		List<Object []> result = query.list(); 
//		result = new ArrayList<>(new LinkedHashSet<>(result));
//		System.out.println(result); 
//		
//		for(Object [] objs: result){
//			System.out.println(Arrays.asList(objs));
//		}
	}
	
	@Test
	public void testLeftJoinFetch(){
//		String hql = "SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.emps";
		String hql = "FROM Department d INNER JOIN FETCH d.emps";
		Query query = session.createQuery(hql);
		
		List<Department> depts = query.list();
		depts = new ArrayList<>(new LinkedHashSet(depts));
		System.out.println(depts.size()); 
		
		for(Department dept: depts){
			System.out.println(dept.getName() + "-" + dept.getEmps().size());
		}
	}
	
	@Test
	public void testGroupBy(){
		String hql = "SELECT min(e.salary), max(e.salary) "
				+ "FROM Employee e "
				+ "GROUP BY e.dept "
				+ "HAVING min(salary) > :minSal";
		
		Query query = session.createQuery(hql)
				             .setFloat("minSal", 8000);
		
		List<Object []> result = query.list();
		for(Object [] objs: result){
			System.out.println(Arrays.asList(objs));
		}
	}
	
	@Test
	public void testFieldQuery2(){
		String hql = "SELECT new Employee(e.email, e.salary, e.dept) "
				+ "FROM Employee e "
				+ "WHERE e.dept = :dept";
		Query query = session.createQuery(hql);
		
		Department dept = new Department();
		dept.setId(80);
		List<Employee> result = query.setEntity("dept", dept)
				                     .list();
		
		for(Employee emp: result){
			System.out.println(emp.getId() + ", " + emp.getEmail() 
					+ ", " + emp.getSalary() + ", " + emp.getDept());
		}
	}
	
	@Test
	public void testFieldQuery(){
		String hql = "SELECT e.email, e.salary, e.dept FROM Employee e WHERE e.dept = :dept";
		Query query = session.createQuery(hql);
		
		Department dept = new Department();
		dept.setId(80);
		List<Object[]> result = query.setEntity("dept", dept)
				                     .list();
		
		for(Object [] objs: result){
			System.out.println(Arrays.asList(objs));
		}
	}
	
	@Test
	public void testNamedQuery(){
		Query query = session.getNamedQuery("salaryEmps");
		
		List<Employee> emps = query.setFloat("minSal", 5000)
				                   .setFloat("maxSal", 10000)
				                   .list();
		
		System.out.println(emps.size()); 
	}
	
	@Test
	public void testPageQuery(){
		String hql = "FROM Employee";
		Query query = session.createQuery(hql);
		
		int pageNo = 22;
		int pageSize = 5;
		
		List<Employee> emps = 
								query.setFirstResult((pageNo - 1) * pageSize)
								     .setMaxResults(pageSize)
								     .list();
		System.out.println(emps);
	}
	
	@Test
	public void testHQLNamedParameter(){
		
		//1. 创建 Query 对象
		//基于命名参数. 
		String hql = "FROM Employee e WHERE e.salary > :sal AND e.email LIKE :email";
		Query query = session.createQuery(hql);
		
		//2. 绑定参数
		query.setFloat("sal", 7000)
		     .setString("email", "%A%");
		
		//3. 执行查询
		List<Employee> emps = query.list();
		System.out.println(emps.size());  
	}
	
	@Test
	public void testHQL(){
		
		//1. 创建 Query 对象
		//基于位置的参数. 
		String hql = "FROM Employee e WHERE e.salary > ? AND e.email LIKE ? AND e.dept = ? "
				+ "ORDER BY e.salary";
		Query query = session.createQuery(hql);
		
		//2. 绑定参数
		//Query 对象调用 setXxx 方法支持方法链的编程风格.
		Department dept = new Department();
		dept.setId(80); 
		query.setFloat(0, 6000)
		     .setString(1, "%A%")
		     .setEntity(2, dept);
		
		//3. 执行查询
		List<Employee> emps = query.list();
		System.out.println(emps.size());  
	}
	
	

}
