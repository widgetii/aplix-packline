package ru.aplix.packline.post;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configuration", namespace = "http://www.aplix.ru/PackingLine")
public class Configuration {

	@XmlElementWrapper(name = "customers", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "customer", namespace = "http://www.aplix.ru/PackingLine")
	private List<Customer> customers;

	@XmlElementWrapper(name = "operators", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "operator", namespace = "http://www.aplix.ru/PackingLine")
	private List<Operator> operators;

	@XmlElementWrapper(name = "orders", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "order", namespace = "http://www.aplix.ru/PackingLine")
	private List<Order> orders;

	@XmlElementWrapper(name = "registries", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "registry", namespace = "http://www.aplix.ru/PackingLine")
	private List<Registry> registries;

	@XmlElementWrapper(name = "incomings", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "incoming", namespace = "http://www.aplix.ru/PackingLine")
	private List<Incoming> incomings;

	@XmlElementWrapper(name = "posts", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "post", namespace = "http://www.aplix.ru/PackingLine")
	private List<Post> posts;

	@XmlElementWrapper(name = "pickupRequests", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "pickupRequest", namespace = "http://www.aplix.ru/PackingLine")
	private List<PickupRequest> pickupRequests;

	@XmlElementWrapper(name = "containers", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "container", namespace = "http://www.aplix.ru/PackingLine")
	private List<Container> containers;

	@XmlElementWrapper(name = "boxTypes", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "boxType", namespace = "http://www.aplix.ru/PackingLine")
	private List<BoxType> boxTypes;

	@XmlElementWrapper(name = "routeLists", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "routeList", namespace = "http://www.aplix.ru/PackingLine")
	private List<RouteList> routeLists;

	@XmlElementWrapper(name = "gatherInfoResponse", namespace = "http://www.aplix.ru/PackingLine")
	@XmlElement(name = "field", required = true)
	protected List<Field> gatherInfoFields;

	public List<Customer> getCustomers() {
		if (customers == null) {
			customers = new ArrayList<Customer>();
		}
		return customers;
	}

	public void setCustomers(List<Customer> customers) {
		this.customers = customers;
	}

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

	public List<PickupRequest> getPickupRequests() {
		if (pickupRequests == null) {
			pickupRequests = new ArrayList<PickupRequest>();
		}
		return pickupRequests;
	}

	public void setPickupRequests(List<PickupRequest> pickupRequests) {
		this.pickupRequests = pickupRequests;
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

	public List<BoxType> getBoxTypes() {
		if (boxTypes == null) {
			boxTypes = new ArrayList<BoxType>();
		}
		return boxTypes;
	}

	public void setBoxTypes(List<BoxType> boxTypes) {
		this.boxTypes = boxTypes;
	}

	public List<Registry> getRegistries() {
		if (registries == null) {
			registries = new ArrayList<Registry>();
		}
		return registries;
	}

	public void setRegistries(List<Registry> registries) {
		this.registries = registries;
	}

	public List<RouteList> getRouteLists() {
		if (routeLists == null) {
			routeLists = new ArrayList<RouteList>();
		}
		return routeLists;
	}

	public void setRouteLists(List<RouteList> routeLists) {
		this.routeLists = routeLists;
	}

	public List<Field> getGatherInfoFields() {
		if (gatherInfoFields == null) {
			gatherInfoFields = new ArrayList<Field>();
		}
		return gatherInfoFields;
	}

	public void setGatherInfoFields(List<Field> gatherInfoFields) {
		this.gatherInfoFields = gatherInfoFields;
	}
}
