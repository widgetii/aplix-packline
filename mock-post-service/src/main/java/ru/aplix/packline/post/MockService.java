package ru.aplix.packline.post;

import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;

@WebService(name = "MockPostService", serviceName = "PackingLine", portName = "PackingLineSoap", endpointInterface = "ru.aplix.packline.post.PackingLinePortType", wsdlLocation = "WEB-INF/wsdl/PackingLine.1cws.wsdl", targetNamespace = "http://www.aplix.ru/PackingLine")
public class MockService implements PackingLinePortType {

	@Override
	public synchronized boolean setOperatorActivity(boolean isActive) {
		// do nothing
		return true;
	}

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
	public GetOperatorResponse2 getOperator2(String machineId) {
		GetOperatorResponse2 response = new GetOperatorResponse2();
		response.setOperator(getOperator(machineId));
		return response;
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
	public synchronized Order getOrder(final String orderId) {
		if (orderId == null) {
			return null;
		}

		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return orderId.equals(((Order) item).getId());
			}
		});
		return order;
	}

	@Override
	public PickupRequestList getPickupRequests(String customerId, XMLGregorianCalendar date) {
		PickupRequestList result = new PickupRequestList();
		result.getItems().addAll(getConfig().getPickupRequests().stream().filter(pr -> customerId.equals(pr.getCustomerId())).collect(Collectors.toList()));
		return result;
	}

	@Override
	public Registry getRegistry(final String registryId) {
		if (registryId == null) {
			return null;
		}

		Registry registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return registryId.equals(((Registry) item).getId());
			}
		});
		return registry;
	}

	@Override
	public synchronized TagType findTag(final String tagId) {
		if (tagId == null) {
			return null;
		}

		final Predicate predicate = new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return tagId.equals(((Tag) item).getId());
			}
		};

		final Incoming incoming = (Incoming) CollectionUtils.find(getConfig().getIncomings(), predicate);
		if (incoming != null) {
			Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getOrderId().equals(((Order) item).getId());
				}
			});

			Registry registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					Registry registry = (Registry) item;
					Incoming incoming = (Incoming) CollectionUtils.find(registry.getIncoming(), predicate);
					return incoming != null;
				}
			});

			if (order != null && order.getTotalIncomings() == 1 && registry != null && registry.isCarriedOutAndClosed()) {
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

		RouteList routeList = (RouteList) CollectionUtils.find(getConfig().getRouteLists(), predicate);
		if (routeList != null) {
			return TagType.ROUTELIST;
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
	public Incoming findIncoming2(String incomingId) {
		return findIncoming(incomingId);
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
	public Container searchContainer(String trackingId) {
		if (trackingId == null) {
			return null;
		}

		Optional<Container> optional = getConfig().getContainers().stream().filter(c -> trackingId.equals(c.getTrackingId())).findFirst();
		return optional.isPresent() ? optional.get() : null;
	}

	@Override
	public RouteList findRouteList(final String routeListId) {
		if (routeListId == null) {
			return null;
		}

		RouteList routeList = (RouteList) CollectionUtils.find(getConfig().getRouteLists(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return routeListId.equals(((Tag) item).getId());
			}
		});
		return routeList;
	}

	@Override
	public Registry findRegistry(String routeListId, final String incomingId) {
		Incoming incoming = findIncoming(incomingId);
		if (incoming == null) {
			return null;
		}

		final Order order = getOrder(incoming.getOrderId());
		if (order == null) {
			return null;
		}

		// First try to find registry with this incoming
		Registry registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				Registry registry = (Registry) item;
				Incoming incoming = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
					@Override
					public boolean evaluate(Object item) {
						return incomingId.equals(((Tag) item).getId());
					}
				});
				return incoming != null;
			}
		});
		if (registry != null) {
			return registry;
		}

		// Then try to find registry with the same customer id
		registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				Registry regItem = (Registry) item;
				return order.getCustomer().getId().equals(regItem.getCustomer().getId()) && !regItem.isCarriedOutAndClosed();
			}
		});

		// If no registry found, create a new one
		if (registry == null) {
			registry = new Registry();
			registry.setId(RandomStringUtils.randomNumeric(10));
			registry.setCustomer(getCustomer(order.getCustomer().getId()));
			registry.setCarriedOutAndClosed(false);
			registry.setTotalIncomings(10);
			registry.setDate(now());
			registry.setRegistryType(RegistryType.INCOMING);
			registry.setActionType(ActionType.ADD);
			getConfig().getRegistries().add(registry);
		}

		return registry;
	}

	@Override
	public Registry findRegistry2(final String registryId, PostType carrier) {
		if (registryId == null) {
			return null;
		}

		return (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				Registry registry = (Registry) item;
				return registryId.equals(registry.getId());
			}
		});
	}

	@Override
	public synchronized int addIncomingToRegistry(final String registryId, final Incoming incoming) {
		// Check order and incoming linkage
		if (registryId == null || incoming == null || incoming.getOrderId() == null) {
			return -1;
		}

		// Find order in global list
		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incoming.getOrderId().equals(((Order) item).getId());
			}
		});
		Registry registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return registryId.equals(((Registry) item).getId());
			}
		});
		if (order != null && registry != null) {
			// Find incoming in the given order
			Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getId().equals(((Tag) item).getId());
				}
			});

			if (existing == null) {
				// If there is no such incoming then add a new one
				existing = new Incoming();
				order.getIncoming().add(existing);
				registry.getIncoming().add(existing);

				// Copy incoming properties
				existing.setId(incoming.getId());
				existing.setOrderId(incoming.getOrderId());
				existing.setPhotoId(incoming.getPhotoId());
				existing.setWeight(incoming.getWeight());
				existing.setContentDescription(incoming.getContentDescription());
				existing.setDate((incoming.getDate() != null) ? (XMLGregorianCalendar) incoming.getDate().clone() : null);
			}

			// Return incoming index in the list
			return registry.getIncoming().indexOf(existing);
		} else {
			return -1;
		}
	}

	@Override
	public synchronized boolean deleteIncomingFromRegistry(final String registryId, final Incoming incoming) {
		if (registryId == null || incoming == null || incoming.getOrderId() == null) {
			return false;
		}

		// Find order in global list
		Order order = (Order) CollectionUtils.find(getConfig().getOrders(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incoming.getOrderId().equals(((Order) item).getId());
			}
		});
		Registry registry = (Registry) CollectionUtils.find(getConfig().getRegistries(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return registryId.equals(((Registry) item).getId());
			}
		});
		if (order != null && registry != null) {
			// Find incoming in the given order
			Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Incoming item = (Incoming) object;
					return incoming.getId().equals(item.getId()) && incoming.getOrderId().equals(item.getOrderId());
				}
			});

			if (existing != null) {
				// Remove incoming from list
				order.getIncoming().remove(existing);
			} else {
				return false;
			}

			// Find incoming in the given registry
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					Incoming item = (Incoming) object;
					return incoming.getId().equals(item.getId()) && incoming.getOrderId().equals(item.getOrderId());
				}
			});

			if (existing != null) {
				// Remove incoming from list
				return registry.getIncoming().remove(existing);
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean deleteRegistry(final String registryId) {
		Registry registry = getRegistry(registryId);
		if (registry != null) {
			// Delete registered incomings from orders first
			for (final Incoming incoming : registry.getIncoming()) {
				for (Order order : getConfig().getOrders()) {
					Incoming incInOrder = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
						@Override
						public boolean evaluate(Object item) {
							return incoming.getId().equals(((Incoming) item).getId());
						}
					});
					if (incInOrder != null) {
						order.getIncoming().remove(incInOrder);
					}
				}
			}

			// Then delete registry itself
			return getConfig().getRegistries().remove(registry);
		} else {
			return false;
		}
	}

	@Override
	public synchronized boolean carryOutRegistry(String registryId) {
		Registry registry = getRegistry(registryId);
		if (registry != null) {
			registry.setCarriedOutAndClosed(true);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int addIncomingToRegistry2(String registryId, Incoming incoming) {
		return addIncomingToRegistry(registryId, incoming);
	}

	@Override
	public boolean deleteIncomingFromRegistry2(String registryId, Incoming incoming) {
		return deleteIncomingFromRegistry(registryId, incoming);
	}

	@Override
	public boolean deleteRegistry2(String registryId) {
		return deleteRegistry(registryId);
	}

	@Override
	public String carryOutRegistry2(String registryId) {
		if (carryOutRegistry(registryId)) {
			return null;
		} else {
			return "Function failed.";
		}
	}

	@Override
	public boolean carryOutRouteList(String routeListId) {
		RouteList routeList = findRouteList(routeListId);
		if (routeList != null) {
			routeList.setCarriedOutAndClosed(true);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean markPostAsShipped(String containerId) {
		Container container = findContainer(containerId);
		if (container != null) {
			container.setShipped(true);
			return true;
		}
		return false;
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
	public UpdateContainerResponse2 updateContainer2(Container container) {
		UpdateContainerResponse2 response = new UpdateContainerResponse2();
		if (updateContainer(container)) {
			response.setContainer(container);
		} else {
			response.setError("Function failed.");
		}
		return response;
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
		for (final String name : fields.getItems()) {
			Field field = new Field();
			field.setName(name);

			if (PostType.class.getSimpleName().equalsIgnoreCase(name)) {
				field.setValue(post.getPostType().name().toUpperCase());
			} else if ("POST_DESCRIPTION".equalsIgnoreCase(name) && post.getEnclosure().size() > 0) {
				field.setValue(post.getEnclosure().get(0).getContentDescription());
			} else if ("CONTAINER_ID".equalsIgnoreCase(name)) {
				field.setValue(containerId);
			} else {
				Field existing = (Field) CollectionUtils.find(getConfig().getGatherInfoFields(), new Predicate() {
					@Override
					public boolean evaluate(Object object) {
						return name.equals(((Field) object).getName());
					}
				});
				if (existing != null) {
					field.setValue(existing.getValue());
				} else {
					field.setValue(RandomStringUtils.randomAlphanumeric(10 + r.nextInt(20)));
				}
			}
			resultList.getItems().add(field);
		}

		return resultList;
	}

	@Override
	public boolean markAsProblem(String containerId, String problemStatus) {
		return true;
	}

	@Override
	public CheckAddressResult checkAddress(String containerId) {
		CheckAddressResult result = new CheckAddressResult();
		result.result = true;
		result.msg = null;
		return result;
	}

	@Override
	public boolean startFillingWarrantyCard(String incomingId, String shipmentId) {
		return true;
	}

	@Override
	public boolean stopFillingWarrantyCard(String incomingId, String shipmentId, boolean filled) {
		return true;
	}

	@Override
	public Post findTrackNumber(String trackId) {
		return null;
	}

	@Override
	public byte[] getLabel(String containerId) {
		try {
			return IOUtils.toByteArray(getClass().getResourceAsStream("/getLabelResponse.bin"));
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public GetLabelResponse2 getLabel2(String containerId) {
		GetLabelResponse2 response = new GetLabelResponse2();
		response.setFileContents(getLabel(containerId));
		return response;
	}

	@Override
	public PrintingDocumentsResponse getDocumentsForPrinting(String postId) {
		PrintingDocumentsResponse response = new PrintingDocumentsResponse();
		if (RandomUtils.nextInt(10) == 0) {
			PrintDocument pd = new PrintDocument();
			pd.setFileContents(getLabel(postId));
			pd.setCopies(1);
			response.getItems().add(pd);
		}
		return response;
	}

	@Override
	public PrintingDocumentsResponse getDocumentsForPrinting2(String postId, TagType tagType) {
		PrintingDocumentsResponse response = new PrintingDocumentsResponse();
		if (RandomUtils.nextInt(10) == 0)
		{
			PrintDocument pd = new PrintDocument();
			pd.setFileContents(getLabel(postId));
			pd.setCopies(1);
			response.getItems().add(pd);
		}
		return response;
	}

	@Override
	public PrintingDocumentsResponse closeAndPrintCurrentRegistry(PostType carrier) {
		PrintingDocumentsResponse response = new PrintingDocumentsResponse();
		PrintDocument pd = new PrintDocument();
		pd.setFileContents(getLabel(null));
		pd.setCopies(1);
		response.getItems().add(pd);
		return response;
	}

	@Override
	public String fileUpload(String fileId, String path, DocumentType document) {
		return null;
	}

	@Override
	public String accountContainerCost(StringList containerIds) {
		return null;
	}

	@Override
	public int getActivePostsCount() {
		return getConfig().getPosts().size();
	}

	@Override
	public PostList getActivePosts() {
		PostList result = new PostList();
		result.getItems().addAll(getConfig().getPosts());
		return result;
	}

	@Override
	public PlannedRegistry getPlannedRegistry() {
		return null;
	}

	@Override
	public String carryOutPlannedRegistry(PlannedRegistryCheckoutItems items, String id) {
		return null;
	}

	@Override
	public String bindRegistryWithPickupRequest(String registryId, String pickupRequestId) {
		Optional<PickupRequest> optionalPickupRequest = configuration.getPickupRequests().stream().filter(pr -> pickupRequestId.equals(pr.getId())).findAny();
		Optional<Registry> optionalRegistry = configuration.getRegistries().stream().filter(r -> registryId.equals(r.getId())).findAny();

		if (optionalRegistry.isPresent() && optionalPickupRequest.isPresent()) {
			optionalRegistry.get().setPickupRequest(optionalPickupRequest.get());
		}

		return null;
	}

	@Override
	public Post createPostFromIncoming(Incoming incoming) {
		Optional<Post> optional = getConfig().getPosts().stream().filter(p -> p.getContainer() != null).findAny();
		return optional.isPresent() ? optional.get() : null;
	}

	@Resource
	private WebServiceContext wsContext;

	private Configuration configuration = null;

	private synchronized Configuration getConfig() {
		if (configuration == null) {
			try {
				ServletContext servletContext = (ServletContext) wsContext.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
				InputStream is = servletContext.getResourceAsStream("/WEB-INF/configuration.xml");

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

	private static XMLGregorianCalendar now() {
		XMLGregorianCalendar xmlGC = null;
		try {
			GregorianCalendar gcal = new GregorianCalendar();
			xmlGC = DatatypeFactory.newInstance().newXMLGregorianCalendar(gcal);
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		return xmlGC;
	}
}
