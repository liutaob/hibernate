package com.atguigu.hibernate.helloworld;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class HibernateTest {
	//1. 创建一个 SessionFactory 对象
	SessionFactory sessionFactory = null;
	Session session = null;
	Transaction transaction = null;

	@Before
	public void init(){
		System.out.println("init...");

		//1). 创建 Configuration 对象: 对应 hibernate 的基本配置信息和 对象关系映射信息
		Configuration configuration = new Configuration().configure("hibernate.cfg.xml");//默认从src下找
		//4.0 之前这样创建
//		sessionFactory = configuration.buildSessionFactory();
		//2). 创建一个 ServiceRegistry 对象: hibernate 4.x 新添加的对象
		//hibernate 的任何配置和服务都需要在该对象中注册后才能有效.
		ServiceRegistry serviceRegistry =
				new ServiceRegistryBuilder().applySettings(configuration.getProperties())
						.buildServiceRegistry();
		//3).
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		//2. 创建一个 Session 对象
		session = sessionFactory.openSession();
		//3. 开启事务
		transaction = session.beginTransaction();
	}

	@After
	public void destory(){
		//5. 提交事务
		transaction.commit();
		//6. 关闭 Session
		session.close();
		//7. 关闭 SessionFactory 对象
		sessionFactory.close();
	}

	@Test
	public void insert() {
		//4. 执行保存操作
//		News news = new News("Java12346", "ATGUIGU", new Date(new java.util.Date().getTime()));
		News news = new News("Java12350", "ATGUIGU", new Date());
//		session.save(news);
		session.persist(news);
	}

	@Test
	public void find(){
//		Object o = session.load(News.class, 123);
		Object o = session.get(News.class, 123);
		System.out.println(o instanceof News);
	}
	
}
