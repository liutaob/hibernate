package com.atguigu.hibernate.strategy;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	public void testMany2OneStrategy(){
//		Order order = (Order) session.get(Order.class, 1);
//		System.out.println(order.getCustomer().getCustomerName()); 
		
		List<Order> orders = session.createQuery("FROM Order o").list();
		for(Order order: orders){
			if(order.getCustomer() != null){
				System.out.println(order.getCustomer().getCustomerName()); 
			}
		}
		
		//1. lazy 取值为 proxy 和 false 分别代表对应对应的属性采用延迟检索和立即检索
		//2. fetch 取值为 join, 表示使用迫切左外连接的方式初始化 n 关联的 1 的一端的属性
		//忽略 lazy 属性. 
		//3. batch-size, 该属性需要设置在 1 那一端的 class 元素中: 
		//<class name="Customer" table="CUSTOMERS" lazy="true" batch-size="5">
		//作用: 一次初始化 1 的这一段代理对象的个数. 
	}
	
	@Test
	public void testSetFetch2(){
		Customer customer = (Customer) session.get(Customer.class, 1);
		System.out.println(customer.getOrders().size()); 
	}
	
	@Test
	public void testSetFetch(){
		List<Customer> customers = session.createQuery("FROM Customer").list();
		
		System.out.println(customers.size()); 
		
		for(Customer customer: customers){
			if(customer.getOrders() != null)
				System.out.println(customer.getOrders().size());
		}
		
		//set 集合的 fetch 属性: 确定初始化 orders 集合的方式. 
		//1. 默认值为 select. 通过正常的方式来初始化 set 元素
		//2. 可以取值为 subselect. 通过子查询的方式来初始化所有的 set 集合. 子查询
		//作为 where 子句的 in 的条件出现, 子查询查询所有 1 的一端的 ID. 此时 lazy 有效.
		//但 batch-size 失效. 
		//3. 若取值为 join. 则
		//3.1 在加载 1 的一端的对象时, 使用迫切左外连接(使用左外链接进行查询, 且把集合属性进行初始化)的方式检索 n 的一端的集合属性
		//3.2 忽略 lazy 属性.
		//3.3 HQL 查询忽略 fetch=join 的取值
	}
	
	@Test
	public void testSetBatchSize(){
		List<Customer> customers = session.createQuery("FROM Customer").list();
		
		System.out.println(customers.size()); 
		
		for(Customer customer: customers){
			if(customer.getOrders() != null)
				System.out.println(customer.getOrders().size());
		}
		
		//set 元素的 batch-size 属性: 设定一次初始化 set 集合的数量. 
	}
	
	@Test
	public void testOne2ManyLevelStrategy(){
		Customer customer = (Customer) session.get(Customer.class, 1);
		System.out.println(customer.getCustomerName()); 
		
		System.out.println(customer.getOrders().size());
		Order order = new Order();
		order.setOrderId(1);
		System.out.println(customer.getOrders().contains(order));
		
		Hibernate.initialize(customer.getOrders()); 
		
		//---------------set 的 lazy 属性------------------
		//1. 1-n 或 n-n 的集合属性默认使用懒加载检索策略.
		//2. 可以通过设置 set 的 lazy 属性来修改默认的检索策略. 默认为 true
		//并不建议设置为  false. 
		//3. lazy 还可以设置为 extra. 增强的延迟检索. 该取值会尽可能的延迟集合初始化的时机!
	}

	//测试类级别检索策略
	@Test
	public void testClassLevelStrategy(){
		Customer customer = (Customer) session.load(Customer.class, 1);
		System.out.println(customer.getClass()); 
		
//		System.out.println(customer.getCustomerId());
//		System.out.println(customer.getCustomerName());
//		System.out.println(customer.getOrders());
	}

	@Test
	public void testMany2OneSave(){
		Customer customer = new Customer();
		customer.setCustomerName("AA");

		Order order1 = new Order();
		order1.setOrderName("ORDER-1");

		Order order2 = new Order();
		order2.setOrderName("ORDER-2");

		//设定关联关系
		order1.setCustomer(customer);
		order2.setCustomer(customer);

		customer.getOrders().add(order1);
		customer.getOrders().add(order2);

		//执行  save 操作: 先插入 Order, 再插入Customer , 3 条 INSERT, 2 条 UPDATE
		//因为 1 的一端和 n 的一端都维护关联关系. 所以会多出 UPDATE
		//可以在 1 的一端的 set 节点指定 inverse=true, 来使 1 的一端放弃维护关联关系!
		//建议设定 set 的 inverse=true, 建议先插入 1 的一端, 后插入多的一端
		//好处是不会多出 UPDATE 语句
		session.save(customer);
		session.save(order1);
		session.save(order2);


		//先插入 Order, 再插入 Cusomer, 3 条 INSERT, 2 条 UPDATE
//		session.save(order1);
//		session.save(order2);
//
//		session.save(customer);
	}
}
