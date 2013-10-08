package ru.aplix.packline.post;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

@WebService(name = "MockPostService", serviceName = "PackingLine", portName = "PackingLineSoap", endpointInterface = "ru.aplix.packline.post.PackingLinePortType", wsdlLocation = "WEB-INF/wsdl/PackingLine.1cws.wsdl", targetNamespace = "http://www.aplix.ru/PackingLine/1.0/ws")
public class MockService implements PackingLinePortType {

	@Override
	public synchronized Operator getOperator(final String operatorId) {
		if (operatorId == null) {
			return null;
		}

		Operator operator = (Operator) CollectionUtils.find(getConfig().getOperators(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return operatorId.equals(((Operator) item).getId());
			}
		});
		return operator;
	}

	@Override
	public synchronized void setOperatorActivity(String operatorId, boolean isActive) {
		// do nothing
	}

	@Override
	public synchronized Tag findTag(final String tagId) {
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
			return order;
		}

		Incoming incoming = (Incoming) CollectionUtils.find(getConfig().getIncomings(), predicate);
		if (incoming != null) {
			return incoming;
		}

		Post post = (Post) CollectionUtils.find(getConfig().getPosts(), predicate);
		if (post != null) {
			return post;
		}

		Container container = (Container) CollectionUtils.find(getConfig().getContainers(), predicate);
		if (container != null) {
			return container;
		}

		return null;
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
				newItem.setPostId(incoming.getPostId());
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
	public synchronized boolean deleteIncomingFromOrder(final String orderId, final String incomingId) {
		if (orderId == null || incomingId == null) {
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
			Incoming incoming = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Incoming item = (Incoming) object;
					return incomingId.equals(item.getId()) && orderId.equals(item.getOrderId());
				}
			});

			if (incoming != null) {
				// Remove incoming from list
				return order.getIncoming().remove(incoming);
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
		return order != null;
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
		if (container == null) {
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

			// Remove the given container from spare list
			Container existing = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return container.getId().equals(((Tag) item).getId());
				}
			});
			if (existing != null) {
				getConfig().getContainers().remove(existing);
			}

			// Return success to the caller
			return true;
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean updateContainer(final Container container) {
		if (container == null) {
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
	public synchronized void setBoxSize(List<Tag> tags, PackingSize packingSize) {
		if (tags == null || packingSize == null) {
			return;
		}

		for (final Tag tag : tags) {
			PackingSize ps = new PackingSize();
			ps.setHeight(packingSize.getHeight());
			ps.setWidth(packingSize.getWidth());
			ps.setLength(packingSize.getLength());

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
	}

	@Override
	public synchronized PackingSize getBoxSize(final String boxId) {
		if (boxId == null) {
			return null;
		}

		// Find the container
		Container container = (Container) CollectionUtils.find(getConfig().getContainers(), new Predicate() {
			@Override
			public boolean evaluate(Object object) {
				Container item = (Container) object;
				return boxId.equals(item.getId()) && PackingType.BOX.equals(item.getPackingType());
			}
		});

		if (container != null) {
			return container.getPackingSize();
		} else {
			return null;
		}
	}

	@Override
	public synchronized int getBoxCount(PackingSize packingSize) {
		if (packingSize == null) {
			return 0;
		}

		int result = 0;
		for (Container container : getConfig().getContainers()) {
			if (container.getPackingSize() != null) {
				PackingSize ps = container.getPackingSize();
				if ((Float.compare(packingSize.getHeight(), ps.getHeight()) == 0) && (Float.compare(packingSize.getWidth(), ps.getWidth()) == 0)
						&& (Float.compare(packingSize.getLength(), ps.getLength()) == 0)) {
					result++;
				}
			}
		}
		return result;
	}

	@Override
	public synchronized List<Tag> generateTags(int count) {
		List<Tag> tags = new ArrayList<Tag>();
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
			tags.add(tag);

			i++;
		}
		return tags;
	}

	@Override
	public synchronized List<Field> gatherInfo(List<String> fields) {
		if (fields == null) {
			return null;
		}

		return null;
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
