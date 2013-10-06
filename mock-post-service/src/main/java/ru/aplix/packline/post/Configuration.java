package ru.aplix.packline.post;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configuration", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
public class Configuration {

	@XmlElementWrapper(name = "operators", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	@XmlElement(name = "operator", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	private List<Operator> operators;

	@XmlElementWrapper(name = "orders", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	@XmlElement(name = "order", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	private List<Order> orders;

	@XmlElementWrapper(name = "incomings", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	@XmlElement(name = "incoming", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	private List<Incoming> incomings;

	@XmlElementWrapper(name = "posts", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	@XmlElement(name = "post", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	private List<Post> posts;

	@XmlElementWrapper(name = "containers", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	@XmlElement(name = "container", namespace = "http://www.aplix.ru/PackingLine/1.0/ws/model")
	private List<Container> containers;

	public List<Operator> getOperators() {
		if (operators == null) {
			operators = new ArrayList<Operator>();
		}
		return operators;
	}

	public void setOperators(List<Operator> operators) {
		this.operators = operators;
	}

	public List<Order> getOrders() {
		if (orders == null) {
			orders = new ArrayList<Order>();
		}
		return orders;
	}

	public void setOrders(List<Order> orders) {
		this.orders = orders;
	}

	public List<Incoming> getIncomings() {
		if (incomings == null) {
			incomings = new ArrayList<Incoming>();
		}
		return incomings;
	}

	public void setIncomings(List<Incoming> incomings) {
		this.incomings = incomings;
	}

	public List<Post> getPosts() {
		if (posts == null) {
			posts = new ArrayList<Post>();
		}
		return posts;
	}

	public void setPosts(List<Post> posts) {
		this.posts = posts;
	}

	public List<Container> getContainers() {
		if (containers == null) {
			containers = new ArrayList<Container>();
		}
		return containers;
	}

	public void setContainers(List<Container> containers) {
		this.containers = containers;
	}
}
