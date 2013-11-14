package ru.aplix.packline.post;

import java.io.InputStream;
import java.util.Random;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.RandomStringUtils;

@WebService(name = "MockPostService", serviceName = "PackingLine", portName = "PackingLineSoap", endpointInterface = "ru.aplix.packline.post.PackingLinePortType", wsdlLocation = "WEB-INF/wsdl/PackingLine.1cws.wsdl", targetNamespace = "http://www.aplix.ru/PackingLine")
public class MockService implements PackingLinePortType {

	@Override
	public synchronized Operator getOperator(String machineId) {
		Random random = new Random();
		int max = getConfig().getOperators().size();
		if (max == 0) {
			return null;
		}
		int index = random.nextInt(max);
		return getConfig().getOperators().get(index);
	}

	@Override
	public Customer getCustomer(final String customerId) {
		if (customerId == null) {
			return null;
		}

		Customer customer = (Customer) CollectionUtils.find(getConfig().getCustomers(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return customerId.equals(((Customer) item).getId());
			}
		});
		return customer;
	}

	@Override
	public synchronized boolean setOperatorActivity(boolean isActive) {
		// do nothing
		return true;
	}

	@Override
	public synchronized TagType findTag(final String tagId) {
		if (tagId == null) {
			return null;
		}

		Predicate predicate = new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return tagId.equals(((Tag) item).getId());
			}
		};

		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), predicate);
		if (order != null) {
			return TagType.ORDER;
		}

		final Incoming incoming = (Incoming) CollectionUtils.find(getConfig().getIncomings(), predicate);
		if (incoming != null) {
			order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getOrderId().equals(((Tag) item).getId());
				}
			});

			if (order != null && order.getTotalIncomings() == 1 && order.isCarriedOutAndClosed()) {
				// go to seeking Post with the same Id
			} else {
				return TagType.INCOMING;
			}
		}

		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), predicate);
		if (post != null) {
			return TagType.POST;
		}

		Container container = (Container) CollectionUtils.find(getConfig().getContainers(), predicate);
		if (container != null) {
			return TagType.CONTAINER;
		}

		return null;
	}

	@Override
	public synchronized Incoming findIncoming(final String incomingId) {
		if (incomingId == null) {
			return null;
		}

		Incoming incoming = (Incoming) CollectionUtils.find(getConfig().getIncomings(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incomingId.equals(((Tag) item).getId());
			}
		});
		return incoming;
	}

	@Override
	public synchronized Order findOrder(final String orderId) {
		if (orderId == null) {
			return null;
		}

		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Tag) item).getId());
			}
		});
		return order;
	}

	@Override
	public synchronized Post findPost(final String postId) {
		if (postId == null) {
			return null;
		}

		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return postId.equals(((Tag) item).getId());
			}
		});
		return post;
	}

	@Override
	public synchronized Container findContainer(final String containerId) {
		if (containerId == null) {
			return null;
		}

		Container container = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return containerId.equals(((Tag) item).getId());
			}
		});
		return container;
	}

	@Override
	public synchronized int addIncomingToOrder(final String orderId, final Incoming incoming) {
		// Check order and incoming linkage
		if (orderId == null || incoming == null || !orderId.equals(incoming.getOrderId())) {
			return -1;
		}

		// Find order in global list
		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Tag) item).getId());
			}
		});
		if (order != null) {
			// Find incoming in the given order
			Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getId().equals(((Tag) item).getId());
				}
			});

			if (existing == null) {
				// If there is no such incoming then add a new one
				Incoming newItem = new Incoming();
				order.getIncoming().add(newItem);

				// Copy incoming properties
				newItem.setId(incoming.getId());
				newItem.setOrderId(incoming.getOrderId());
				newItem.setPhotoId(incoming.getPhotoId());
				newItem.setWeight(incoming.getWeight());
				newItem.setContentDescription(incoming.getContentDescription());
				newItem.setDate((incoming.getDate() != null) ? (XMLGregorianCalendar) incoming.getDate().clone() : null);

				// Return incoming index in the list
				return order.getIncoming().indexOf(newItem);
			} else {
				return -1;
			}
		} else {
			return -1;
		}
	}

	@Override
	public synchronized boolean deleteIncomingFromOrder(final String orderId, final Incoming incoming) {
		if (orderId == null || incoming == null || incoming.getId() == null) {
			return false;
		}

		// Find order in global list
		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Tag) item).getId());
			}
		});

		if (order != null) {
			// Find incoming in the given order
			Incoming incoming2 = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Incoming item = (Incoming) object;
					return incoming.getId().equals(item.getId()) && orderId.equals(item.getOrderId());
				}
			});

			if (incoming2 != null) {
				// Remove incoming from list
				return order.getIncoming().remove(incoming2);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean carryOutOrder(final String orderId) {
		if (orderId == null) {
			return false;
		}

		// Find order in global list
		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Tag) item).getId());
			}
		});

		if (order != null) {
			order.setCarriedOutAndClosed(true);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean deleteOrder(final String orderId) {
		if (orderId == null) {
			return false;
		}

		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Tag) item).getId());
			}
		});
		if (order != null) {
			return getConfig().getOrders().remove(order);
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean addContainer(final Container container) {
		if (container == null || container.getPostId() == null) {
			return false;
		}

		// Find the post to which the given container belongs
		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return container.getPostId().equals(((Tag) item).getId());
			}
		});

		if (post != null) {
			// Assign the given container with found post
			Container newItem = new Container();
			post.setContainer(newItem);

			// Copy container properties
			newItem.setId(container.getId());
			newItem.setPostId(container.getPostId());
			newItem.setPackingType(container.getPackingType());
			newItem.setTotalWeight(container.getTotalWeight());
			newItem.setDate((container.getDate() != null) ? (XMLGregorianCalendar) container.getDate().clone() : null);

			PackingSize newPackingSize = null;
			if (container.getPackingSize() != null) {
				newPackingSize = new PackingSize();
				newPackingSize.setHeight(container.getPackingSize().getHeight());
				newPackingSize.setWidth(container.getPackingSize().getWidth());
				newPackingSize.setLength(container.getPackingSize().getLength());
			}
			newItem.setPackingSize(newPackingSize);

			// Associate the container from spare list with that post
			Container existing = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return container.getId().equals(((Tag) item).getId());
				}
			});
			if (existing != null) {
				existing.setPostId(post.getId());
			}

			// Return success to the caller
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean updateContainer(final Container container) {
		if (container == null || container.getPostId() == null) {
			return false;
		}

		// Find the post to which the given container belongs
		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return container.getPostId().equals(((Tag) item).getId());
			}
		});

		if (post != null && post.getContainer() != null) {
			// Get assigned container
			Container existing = post.getContainer();

			// Update container properties
			PackingSize newPackingSize = null;
			if (container.getPackingSize() != null) {
				newPackingSize = new PackingSize();
				newPackingSize.setHeight(container.getPackingSize().getHeight());
				newPackingSize.setWidth(container.getPackingSize().getWidth());
				newPackingSize.setLength(container.getPackingSize().getLength());
			}
			existing.setId(container.getId());
			existing.setPackingSize(newPackingSize);
			existing.setPackingType(container.getPackingType());
			existing.setPostId(container.getPostId());
			existing.setTotalWeight(container.getTotalWeight());
			existing.setDate((container.getDate() != null) ? (XMLGregorianCalendar) container.getDate().clone() : null);

			// Return success to the caller
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean addBoxContainers(final String boxTypeId, TagList tagList) {
		if (tagList == null || boxTypeId == null) {
			return false;
		}

		// Verify this is is not used yet
		BoxType boxType = (BoxType) CollectionUtils.find(getConfig().getBoxTypes(), new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				BoxType item = (BoxType) object;
				return boxTypeId.equals(item.getId());
			}
		});

		if (boxType == null) {
			return false;
		}

		for (final Tag tag : tagList.getItems()) {
			PackingSize ps = new PackingSize();
			ps.setHeight(boxType.getPackingSize().getHeight());
			ps.setWidth(boxType.getPackingSize().getWidth());
			ps.setLength(boxType.getPackingSize().getLength());

			// Verify this is is not used yet
			Container container = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Container item = (Container) object;
					return tag.getId().equals(item.getId());
				}
			});

			if (container == null) {
				container = new Container();
				container.setId(tag.getId());
				getConfig().getContainers().add(container);
			}

			container.setPackingSize(ps);
			container.setPackingType(PackingType.BOX);
		}

		return true;
	}

	@Override
	public synchronized BoxType getBoxType(final String boxTypeId) {
		if (boxTypeId == null) {
			return null;
		}

		// Find the box
		BoxType boxType = (BoxType) CollectionUtils.find(getConfig().getBoxTypes(), new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				BoxType item = (BoxType) object;
				return boxTypeId.equals(item.getId());
			}
		});

		return boxType;
	}

	@Override
	public synchronized int getBoxCount(final String boxTypeId) {
		if (boxTypeId == null) {
			return 0;
		}

		int result = 0;
		for (Container container : getConfig().getContainers()) {
			if (boxTypeId.equals(container.getBoxTypeId()) && container.getPostId() == null) {
				result++;
			}
		}
		return result;
	}

	@Override
	public synchronized TagList generateTagsForContainers(int count) {
		TagList tagList = new TagList();
		Random r = new Random();
		int i = 0;
		while (i < count) {
			// Generate new id for container
			final String id = "" + 100000 + r.nextInt(100000);

			// Verify this is is not used yet
			Container container = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Container item = (Container) object;
					return id.equals(item.getId());
				}
			});

			// If id already exists then generate a new one
			if (container != null) {
				continue;
			}

			// Add new id to returned list
			Tag tag = new Tag();
			tag.setId(id);
			tagList.getItems().add(tag);

			i++;
		}
		return tagList;
	}

	@Override
	public TagList generateTagsForIncomings(String customerId, int count) {
		return generateTagsForContainers(count);
	}

	@Override
	public synchronized FieldList gatherInfo(final String containerId, StringList fields) {
		if (fields == null) {
			return null;
		}

		final Container container = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				Container item = (Container) object;
				return containerId.equals(item.getId());
			}
		});

		FieldList resultList = new FieldList();
		if (container == null) {
			return resultList;
		}

		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return container.getPostId().equals(((Tag) item).getId());
			}
		});

		Random r = new Random();
		for (String name : fields.getItems()) {
			Field field = new Field();
			field.setName(name);

			if (PostType.class.getSimpleName().equalsIgnoreCase(name)) {
				field.setValue(post.getPostType().name().toUpperCase());
			} else {
				field.setValue(RandomStringUtils.randomAlphanumeric(10 + r.nextInt(20)));
			}
			resultList.getItems().add(field);
		}

		return resultList;
	}

	@Override
	public String echo(String text) {
		return text;
	}

	@Resource
	private WebServiceContext wsContext;

	private Configuration configuration = null;

	private synchronized Configuration getConfig() {
		if (configuration == null) {
			try {
				ServletContext servletContext = (ServletContext) wsContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
				InputStream is = servletContext.getResourceAsStream("WEB-INF/configuration.xml");

				JAXBContext inst = JAXBContext.newInstance(Configuration.class);
				Unmarshaller unmarshaller = inst.createUnmarshaller();
				configuration = (Configuration) unmarshaller.unmarshal(is);
			} catch (Exception e) {
				throw new RuntimeException("Error reading configuration.", e);
			}
		}
		return configuration;
	}

	// It's not a good idea to mark any method of a web-service as synchronized,
	// but as soon as we use reentrance approach (like with ThreadLocal), the
	// multiple copies of Configuration don't keep the changes. So, we make
	// such assumption only for mock implementation.

	// @formatter:off
	/* private final ThreadLocal<Configuration> configuration = new ThreadLocal<Configuration>() {

		@Override
		protected Configuration initialValue() {
			try {
				ServletContext servletContext = (ServletContext) wsContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
				InputStream is = servletContext.getResourceAsStream("WEB-INF/configuration.xml");

				JAXBContext inst = JAXBContext.newInstance(Configuration.class);
				Unmarshaller unmarshaller = inst.createUnmarshaller();
				return (Configuration) unmarshaller.unmarshal(is);
			} catch (Exception e) {
				throw new RuntimeException("Error reading configuration.", e);
			}
		};
	}; */
	// @formatter:on
}
