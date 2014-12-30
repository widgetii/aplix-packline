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
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.Duration;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ActivePostsAction;
import ru.aplix.packline.action.ActivePostsAction.CustomerItem;
import ru.aplix.packline.action.ActivePostsAction.OrderItem;
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

	private Timeline updater;
	private Task<?> task;

	public ActivePostsController() throws FileNotFoundException, MalformedURLException, JAXBException {
		super();

		dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

		updater = new Timeline(new KeyFrame(Duration.ZERO, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				doAction();
			}
		}), new KeyFrame(Duration.minutes(Math.max(Configuration.getInstance().getActivePostsUpdateInterval(), 1))));
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

		updater.playFromStart();
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
				String s = "";
				if (p.getValue().getValue().getPostType() != null) {
					s = p.getValue().getValue().getPostType().toString();
				}
				return new ReadOnlyObjectWrapper<String>(s);
			}
		});

		column = postsTreeTableView.getColumns().get(6);
		column.prefWidthProperty().bind(postsTreeTableView.widthProperty().multiply(0.10));
		column.setCellValueFactory(new Callback<CellDataFeatures<TableColumns, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<TableColumns, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getValue().getContainerId());
			}
		});

		postsTreeTableView.setPlaceholder(new Text(getResources().getString("activeposts.noactiveposts")));
	}

	private void updateTable(List<CustomerItem> list) {
		TreeItem<TableColumns> rootNode = new TreeItem<TableColumns>();

		for (CustomerItem customerItem : list) {
			TreeItem<TableColumns> customerNode = new TreeItem<TableColumns>(new CustomerItemColumns(customerItem));
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

		postsTreeTableView.setRoot(rootNode);
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
	}

	private void doAction() {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<List<CustomerItem>>() {
			@Override
			public List<CustomerItem> call() throws Exception {
				try {
					return getAction().getActivePosts();
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
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
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
			return customerItem.getCustomer().getName();
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
			return String.format(getResources().getString("activeposts.order"), orderItem.getOrder().getId());
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
			return null;
		}

		@Override
		public String getDeliveryAddress() {
			return orderItem.getOrder().getDeliveryAddress();
		}

		@Override
		public String getConsigneeName() {
			return orderItem.getOrder().getClientName();
		}

		@Override
		public String getContainerId() {
			return null;
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
			return String.format(getResources().getString("activeposts.post"), post.getId());
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
		public String getConsigneeName() {
			return null;
		}

		@Override
		public String getContainerId() {
			return post.getContainer() != null ? post.getContainer().getId() : null;
		}
	}
}
