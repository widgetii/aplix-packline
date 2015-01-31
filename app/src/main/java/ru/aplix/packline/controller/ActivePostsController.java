package ru.aplix.packline.controller;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.SortEvent;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ActivePostsAction;
import ru.aplix.packline.action.ActivePostsAction.CustomerItem;
import ru.aplix.packline.action.ActivePostsAction.GroupItem;
import ru.aplix.packline.action.ActivePostsAction.OrderItem;
import ru.aplix.packline.action.ActivePostsAction.PostTypeItem;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.PostType;
import ru.aplix.packline.workflow.WorkflowContext;

public class ActivePostsController extends StandardController<ActivePostsAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private TreeTableView<TableColumns> postsTreeTableView;

	private DateFormat dateFormat;
	private DateFormat timeFormat;
	private GroupType groupType = GroupType.CUSTOMER;

	private Timeline updater;
	private Task<?> task;

	public ActivePostsController() throws FileNotFoundException, MalformedURLException, JAXBException {
		super();

		dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

		updater = new Timeline(new KeyFrame(Duration.minutes(Math.max(Configuration.getInstance().getActivePostsUpdateInterval(), 1)),
				new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						doAction();
					}
				}));
		updater.setCycleCount(Timeline.INDEFINITE);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		initTable();
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		doAction();
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		updater.stop();

		if (task != null) {
			task.cancel(false);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void initTable() {
		TreeTableColumn column = postsTreeTableView.getColumns().get(0);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.20));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				return new ReadOnlyStringWrapper(p.getValue().getValue().getId());
			}
		});

		column = postsTreeTableView.getColumns().get(1);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.10));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				String s = "";
				if (p.getValue().getValue().getDate() != null) {
					Date d = p.getValue().getValue().getDate().toGregorianCalendar().getTime();
					s = String.format("%s %s", dateFormat.format(d), timeFormat.format(d));
				}
				return new ReadOnlyObjectWrapper<String>(s);
			}
		});

		column = postsTreeTableView.getColumns().get(2);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.05));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, Integer>, ObservableValue<Integer>>() {
			@Override
			public ObservableValue<Integer> call(CellDataFeatures<TableColumns, Integer> p) {
				return new ReadOnlyObjectWrapper<Integer>(p.getValue().getValue().getCount());
			}
		});

		column = postsTreeTableView.getColumns().get(3);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.20));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getValue().getDeliveryAddress());
			}
		});

		column = postsTreeTableView.getColumns().get(4);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.20));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getValue().getConsigneeName());
			}
		});

		column = postsTreeTableView.getColumns().get(5);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.10));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				if (GroupType.CUSTOMER.equals(groupType)) {
					String s = "";
					if (p.getValue().getValue().getPostType() != null) {
						s = p.getValue().getValue().getPostType().toString();
					}
					return new ReadOnlyObjectWrapper<String>(s);
				} else {
					return new ReadOnlyObjectWrapper<String>(p.getValue().getValue().getCustomerName());
				}
			}
		});
		column.setSortable(true);

		column = postsTreeTableView.getColumns().get(6);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.10));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getValue().getContainerId());
			}
		});

		postsTreeTableView.setPlaceholder(new Text(getResources().getString("activeposts.noactiveposts")));
		postsTreeTableView.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				updater.playFromStart();
			}
		});
		postsTreeTableView.setOnSort(new EventHandler<SortEvent<TreeTableView<TableColumns>>>() {
			@Override
			public void handle(SortEvent<TreeTableView<TableColumns>> event) {
				event.consume();

				changeGroupType();
			}
		});
	}

	private void updateTable(List<? extends GroupItem<?>> list) {
		TreeItem<TableColumns> rootNode = new TreeItem<TableColumns>();

		for (GroupItem<?> customerItem : list) {
			TableColumns tableColumns;

			if (customerItem instanceof CustomerItem) {
				tableColumns = new CustomerItemColumns((CustomerItem) customerItem);
			} else if (customerItem instanceof PostTypeItem) {
				tableColumns = new PostTypeItemColumns((PostTypeItem) customerItem);
			} else {
				continue;
			}

			TreeItem<TableColumns> customerNode = new TreeItem<TableColumns>(tableColumns);
			rootNode.getChildren().add(customerNode);

			for (OrderItem orderItem : customerItem.getOrders()) {
				TreeItem<TableColumns> orderNode = new TreeItem<TableColumns>(new OrderItemColumns(orderItem));
				customerNode.getChildren().add(orderNode);

				for (Post post : orderItem.getPosts()) {
					TreeItem<TableColumns> postNode = new TreeItem<TableColumns>(new PostItemColumns(post));
					orderNode.getChildren().add(postNode);
				}
			}
		}

		EventHandler<SortEvent<TreeTableView<TableColumns>>> oldEventListener = postsTreeTableView.getOnSort();
		postsTreeTableView.setOnSort(null);
		postsTreeTableView.setRoot(rootNode);
		postsTreeTableView.setOnSort(oldEventListener);

		switch (groupType) {
		case CUSTOMER:
			postsTreeTableView.getColumns().get(0).setText(getResources().getString("activeposts.customer"));
			postsTreeTableView.getColumns().get(5).setText(getResources().getString("activeposts.carrier"));
			break;
		case POSTTYPE:
			postsTreeTableView.getColumns().get(0).setText(getResources().getString("activeposts.carrier"));
			postsTreeTableView.getColumns().get(5).setText(getResources().getString("activeposts.customer"));
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		if (value) {
			updater.pause();
		} else {
			updater.play();
		}
	}

	private void doAction() {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<List<? extends GroupItem<?>>>() {
			@Override
			public List<? extends GroupItem<?>> call() throws Exception {
				try {
					if (GroupType.CUSTOMER.equals(groupType)) {
						return getAction().getActivePostsByCustomer();
					} else {
						return getAction().getActivePostsByCarrier();
					}
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				setProgress(true);
			}

			@Override
			protected void failed() {
				super.failed();

				setProgress(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				updateTable(getValue());
				getAction().updatePostsCount("" + getAction().getTotalPostsCount());
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void changeGroupType() {
		if (GroupType.CUSTOMER.equals(groupType)) {
			groupType = GroupType.POSTTYPE;
		} else {
			groupType = GroupType.CUSTOMER;
		}
		doAction();
	}

	/**
	 * 
	 */
	private interface TableColumns {

		String getId();

		XMLGregorianCalendar getDate();

		Integer getCount();

		PostType getPostType();

		String getDeliveryAddress();

		String getCustomerName();

		String getConsigneeName();

		String getContainerId();
	}

	/**
	 * 
	 */
	private class CustomerItemColumns implements TableColumns {

		private CustomerItem customerItem;

		public CustomerItemColumns(CustomerItem customerItem) {
			this.customerItem = customerItem;
		}

		@Override
		public String getId() {
			return customerItem.getGroup().getName();
		}

		@Override
		public XMLGregorianCalendar getDate() {
			return customerItem.getMinDate();
		}

		@Override
		public Integer getCount() {
			return customerItem.getPostsCount();
		}

		@Override
		public PostType getPostType() {
			return null;
		}

		@Override
		public String getDeliveryAddress() {
			return null;
		}

		@Override
		public String getCustomerName() {
			return customerItem.getGroup().getName();
		}

		@Override
		public String getConsigneeName() {
			return null;
		}

		@Override
		public String getContainerId() {
			return null;
		}

	}

	/**
	 * 
	 */
	private class PostTypeItemColumns implements TableColumns {

		private PostTypeItem postTypeItem;

		public PostTypeItemColumns(PostTypeItem postTypeItem) {
			this.postTypeItem = postTypeItem;
		}

		@Override
		public String getId() {
			return postTypeItem.getGroup().toString();
		}

		@Override
		public XMLGregorianCalendar getDate() {
			return postTypeItem.getMinDate();
		}

		@Override
		public Integer getCount() {
			return postTypeItem.getPostsCount();
		}

		@Override
		public PostType getPostType() {
			return null;
		}

		@Override
		public String getDeliveryAddress() {
			return null;
		}

		@Override
		public String getCustomerName() {
			return null;
		}

		@Override
		public String getConsigneeName() {
			return null;
		}

		@Override
		public String getContainerId() {
			return null;
		}
	}

	/**
	 * 
	 */
	private class OrderItemColumns implements TableColumns {

		private OrderItem orderItem;

		public OrderItemColumns(OrderItem orderItem) {
			this.orderItem = orderItem;
		}

		@Override
		public String getId() {
			return String.format(getResources().getString("activeposts.order"), StringUtils.isEmpty(orderItem.getOrder().getId()) ? "" : orderItem.getOrder()
					.getId());
		}

		@Override
		public XMLGregorianCalendar getDate() {
			return orderItem.getOrder().getDate();
		}

		@Override
		public Integer getCount() {
			return orderItem.getPosts().size();
		}

		@Override
		public PostType getPostType() {
			return getCount() == 1 ? orderItem.getPosts().get(0).getPostType() : null;
		}

		@Override
		public String getDeliveryAddress() {
			return orderItem.getOrder().getDeliveryAddress();
		}

		@Override
		public String getCustomerName() {
			return orderItem.getOrder().getCustomer() != null ? orderItem.getOrder().getCustomer().getName() : null;
		}

		@Override
		public String getConsigneeName() {
			return orderItem.getOrder().getClientName();
		}

		@Override
		public String getContainerId() {
			if (getCount() == 1) {
				Post post = orderItem.getPosts().get(0);
				return post.getContainer() != null ? post.getContainer().getId() : null;
			} else {
				return null;
			}
		}
	}

	/**
	 * 
	 */
	private class PostItemColumns implements TableColumns {

		private Post post;

		public PostItemColumns(Post post) {
			this.post = post;
		}

		@Override
		public String getId() {
			return String.format(getResources().getString("activeposts.post"), StringUtils.isEmpty(post.getId()) ? "" : post.getId());
		}

		@Override
		public XMLGregorianCalendar getDate() {
			return post.getDate();
		}

		@Override
		public Integer getCount() {
			return null;
		}

		@Override
		public PostType getPostType() {
			return post.getPostType();
		}

		@Override
		public String getDeliveryAddress() {
			return null;
		}

		@Override
		public String getCustomerName() {
			return null;
		}

		@Override
		public String getConsigneeName() {
			return null;
		}

		@Override
		public String getContainerId() {
			return post.getContainer() != null ? post.getContainer().getId() : null;
		}
	}

	/**
	 * 
	 */
	private enum GroupType {
		CUSTOMER, POSTTYPE
	}
}
