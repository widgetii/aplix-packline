package ru.aplix.packline.controller;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.adapter.JavaBeanBooleanPropertyBuilder;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.text.Text;
import javafx.util.Callback;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.action.SelectPrintFormsAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.PrintForm;

public class SelectPrintFormsController extends StandardController<SelectPrintFormsAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView printFormsView;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		try {
			initTable();
		} catch (Throwable e) {
			LOG.error(null, e);
		}
	}

	@SuppressWarnings("unchecked")
	private void initTable() throws FileNotFoundException, MalformedURLException, JAXBException {
		TableColumn<PrintForm, Boolean> firstColumn = new TableColumn<PrintForm, Boolean>(getResources().getString("remarking.print"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(true);
		firstColumn.prefWidthProperty().bind(printFormsView.widthProperty().multiply(0.10));
		firstColumn.setCellValueFactory(new Callback<CellDataFeatures<PrintForm, Boolean>, ObservableValue<Boolean>>() {
			@Override
			public ObservableValue<Boolean> call(CellDataFeatures<PrintForm, Boolean> p) {
				try {
					return JavaBeanBooleanPropertyBuilder.create().bean(p.getValue()).name("enabled").build();
				} catch (NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}
		});
		firstColumn.setCellFactory(new Callback<TableColumn<PrintForm, Boolean>, TableCell<PrintForm, Boolean>>() {
			@Override
			public TableCell<PrintForm, Boolean> call(TableColumn<PrintForm, Boolean> p) {
				return new CheckBoxTableCell<PrintForm, Boolean>();
			}
		});

		TableColumn<PrintForm, String> secondColumn = new TableColumn<PrintForm, String>(getResources().getString("remarking.formname"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(printFormsView.widthProperty().multiply(0.80));
		secondColumn.setCellValueFactory(new Callback<CellDataFeatures<PrintForm, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<PrintForm, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getName());
			}
		});

		printFormsView.getColumns().add(firstColumn);
		printFormsView.getColumns().add(secondColumn);
		printFormsView.setPlaceholder(new Text(getResources().getString("remarking.noforms")));
		printFormsView.setEditable(true);

		ObservableList<PrintForm> data = FXCollections.observableArrayList(Configuration.getInstance().getPrintForms());
		printFormsView.setItems(data);
	}

	public void nextClick(ActionEvent event) {
		done();
	}
}
