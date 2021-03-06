package ru.aplix.packline.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ActivePostsController;
import ru.aplix.packline.post.Customer;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.PostList;
import ru.aplix.packline.post.PostType;

public class ActivePostsAction extends CommonAction<ActivePostsController> {

	private Integer totalPostsCount;

	@Override
	protected String getFormName() {
		return "active-posts";
	}

	@Override
	protected void updatePostsCount() {
		// Do nothing here because we will update counter in another method
	}

	public Integer getTotalPostsCount() {
		return totalPostsCount;
	}

	public List<CustomerItem> getActivePostsByCustomer() throws PackLineException {
		// Get active posts
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		PostList postList = postServicePort.getActivePosts();
		if (postList == null) {
			throw new PackLineException(getResources().getString("error.post.active.posts"));
		}

		// Build result list
		List<CustomerItem> resultList = new ArrayList<CustomerItem>();
		totalPostsCount = 0;
		for (Post post : postList.getItems()) {
			// Retrieve corresponding order
			final Order order = postServicePort.getOrder(post.getOrderId());
			if (order == null || order.getId() == null || order.getId().length() == 0) {
				throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
			}

			// Find customer item and create a new one of not found
			CustomerItem customerItem = (CustomerItem) CollectionUtils.find(resultList, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					CustomerItem item = (CustomerItem) object;
					return StringUtils.equalsIgnoreCase(item.getGroup().getName(), order.getCustomer().getName());
				}
			});
			if (customerItem == null) {
				customerItem = new CustomerItem();
				customerItem.group = order.getCustomer();
				resultList.add(customerItem);
			}

			// Find order item and create a new one of not found
			OrderItem orderItem = (OrderItem) CollectionUtils.find(customerItem.getOrders(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					OrderItem item = (OrderItem) object;
					return StringUtils.equalsIgnoreCase(item.getOrder().getId(), order.getId());
				}
			});
			if (orderItem == null) {
				orderItem = new OrderItem();
				orderItem.order = order;
				customerItem.getOrders().add(orderItem);
			}

			// Add post to the found order item
			orderItem.getPosts().add(post);

			if (customerItem.minDate == null || customerItem.minDate.compare(post.getDate()) == 1) {
				customerItem.minDate = post.getDate();
			}

			customerItem.postsCount++;
			totalPostsCount++;
		}

		// Sort lists by date
		for (CustomerItem customerItem : resultList) {
			Collections.sort(customerItem.getOrders(), new Comparator<OrderItem>() {
				@Override
				public int compare(OrderItem o1, OrderItem o2) {
					if (o1.getOrder().getDate() == null && o2.getOrder().getDate() == null) {
						return 0;
					} else if (o1.getOrder().getDate() == null && o2.getOrder().getDate() != null) {
						return -1;
					} else {
						return o1.getOrder().getDate().compare(o2.getOrder().getDate());
					}
				}
			});

			for (OrderItem orderItem : customerItem.getOrders()) {
				Collections.sort(orderItem.getPosts(), new Comparator<Post>() {
					@Override
					public int compare(Post p1, Post p2) {
						if (p1.getDate() == null && p2.getDate() == null) {
							return 0;
						} else if (p1.getDate() == null && p2.getDate() != null) {
							return -1;
						} else {
							return p1.getDate().compare(p2.getDate());
						}
					}
				});
			}
		}

		Collections.sort(resultList, new Comparator<CustomerItem>() {
			@Override
			public int compare(CustomerItem c1, CustomerItem c2) {
				if (c1.getMinDate() == null && c2.getMinDate() == null) {
					return 0;
				} else if (c1.getMinDate() == null && c2.getMinDate() != null) {
					return -1;
				} else {
					return c1.getMinDate().compare(c2.getMinDate());
				}
			}
		});

		sortByDates(resultList);
		return resultList;
	}

	public List<PostTypeItem> getActivePostsByCarrier() throws PackLineException {
		// Get active posts
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		PostList postList = postServicePort.getActivePosts();
		if (postList == null) {
			throw new PackLineException(getResources().getString("error.post.active.posts"));
		}

		// Build result list
		List<PostTypeItem> resultList = new ArrayList<PostTypeItem>();
		totalPostsCount = 0;
		for (final Post post : postList.getItems()) {
			// Find customer item and create a new one of not found
			PostTypeItem postTypeItem = (PostTypeItem) CollectionUtils.find(resultList, new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					PostTypeItem item = (PostTypeItem) object;
					return item.getGroup().equals(post.getPostType());
				}
			});
			if (postTypeItem == null) {
				postTypeItem = new PostTypeItem();
				postTypeItem.group = post.getPostType();
				resultList.add(postTypeItem);
			}

			// Retrieve corresponding order
			final Order order = postServicePort.getOrder(post.getOrderId());
			if (order == null || order.getId() == null || order.getId().length() == 0) {
				throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
			}

			// Find order item and create a new one of not found
			OrderItem orderItem = (OrderItem) CollectionUtils.find(postTypeItem.getOrders(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					OrderItem item = (OrderItem) object;
					return StringUtils.equalsIgnoreCase(item.getOrder().getId(), order.getId());
				}
			});
			if (orderItem == null) {
				orderItem = new OrderItem();
				orderItem.order = order;
				postTypeItem.getOrders().add(orderItem);
			}

			// Add post to the found order item
			orderItem.getPosts().add(post);

			if (postTypeItem.minDate == null || postTypeItem.minDate.compare(post.getDate()) == 1) {
				postTypeItem.minDate = post.getDate();
			}

			postTypeItem.postsCount++;
			totalPostsCount++;
		}

		sortByDates(resultList);
		return resultList;
	}

	private <T> void sortByDates(List<? extends GroupItem<T>> list) {
		// Sort lists by date
		for (GroupItem<?> customerItem : list) {
			Collections.sort(customerItem.getOrders(), new Comparator<OrderItem>() {
				@Override
				public int compare(OrderItem o1, OrderItem o2) {
					if (o1.getOrder().getDate() == null && o2.getOrder().getDate() == null) {
						return 0;
					} else if (o1.getOrder().getDate() == null && o2.getOrder().getDate() != null) {
						return -1;
					} else {
						return o1.getOrder().getDate().compare(o2.getOrder().getDate());
					}
				}
			});

			for (OrderItem orderItem : customerItem.getOrders()) {
				Collections.sort(orderItem.getPosts(), new Comparator<Post>() {
					@Override
					public int compare(Post p1, Post p2) {
						if (p1.getDate() == null && p2.getDate() == null) {
							return 0;
						} else if (p1.getDate() == null && p2.getDate() != null) {
							return -1;
						} else {
							return p1.getDate().compare(p2.getDate());
						}
					}
				});
			}
		}

		Collections.sort(list, new Comparator<GroupItem<?>>() {
			@Override
			public int compare(GroupItem<?> c1, GroupItem<?> c2) {
				if (c1.getMinDate() == null && c2.getMinDate() == null) {
					return 0;
				} else if (c1.getMinDate() == null && c2.getMinDate() != null) {
					return -1;
				} else {
					return c1.getMinDate().compare(c2.getMinDate());
				}
			}
		});
	}

	/**
	 * 
	 */
	public class CustomerItem extends GroupItem<Customer> {

	}

	/**
	 * 
	 */
	public class PostTypeItem extends GroupItem<PostType> {

	}

	/**
	 * 
	 */
	public class GroupItem<T> {

		protected T group;
		protected List<OrderItem> orders;
		protected XMLGregorianCalendar minDate;
		protected int postsCount;

		public T getGroup() {
			return group;
		}

		public List<OrderItem> getOrders() {
			if (orders == null) {
				orders = new ArrayList<OrderItem>();
			}
			return orders;
		}

		public XMLGregorianCalendar getMinDate() {
			return minDate;
		}

		public int getPostsCount() {
			return postsCount;
		}

	}

	/**
	 * 
	 */
	public class OrderItem {

		private Order order;
		private List<Post> posts;

		public Order getOrder() {
			return order;
		}

		public List<Post> getPosts() {
			if (posts == null) {
				posts = new ArrayList<Post>();
			}
			return posts;
		}

	}

}
