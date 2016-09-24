package com.starterkit.javafx.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.DirectoryChooser;

public class ImageViewerController {

	private static final long CALL_DELAY = 1500;

	private static final double MAX_SCALE = 4;
	private static final double MIN_SCALE = 3;

	private boolean stopSlideShow;
	protected static final double SCALE_FACTOR = 1.2;

	final DoubleProperty zoomProperty = new SimpleDoubleProperty(200);

	private static final Logger LOG = Logger.getLogger(ImageViewerController.class);
	// REV: sciekza nie moze byc zahardcodowana
	File filePath = new File(
			"C:/Projects/StarterKit/javafx/zadanie2/javafx/src/main/resources/com/starterkit/javafx/imagetest/");

	@FXML
	private ResourceBundle resources;

	@FXML
	private URL location;

	@FXML
	private TextField nameField;

	@FXML
	private Button openButton;

	@FXML
	private Button nextButton;

	@FXML
	private Button prevButton;

	@FXML
	private Button slideShowButton;

	@FXML
	private Button stopSlideShowButton;

	@FXML
	private ImageView imageView;

	@FXML
	private ListView<Path> listView;

	@FXML
	private ScrollPane scrollPane;

	public ImageViewerController() {
		LOG.debug("Constructor: nameField = " + nameField);
	}

	@FXML
	private void initialize() throws FileNotFoundException {
		LOG.debug("initialize(): nameField = " + nameField);

		imageView.setImage(null);
		nextButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		prevButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		slideShowButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		stopSlideShowButton.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
		listView.disableProperty().bind(nameField.textProperty().isEmpty());
		imageView.disableProperty().bind(nameField.textProperty().isEmpty());
		MouseEvent arg0 = null;
		handleZoomScroll(arg0);

	};

	@FXML
	public void handleOpenButonClick(MouseEvent arg0) {
		LOG.debug("Entering OpenButonClick()");
		ObservableList<Path> imageFiles = FXCollections.observableArrayList();
		
		DirectoryChooser directoryChooser = new DirectoryChooser();
		// REV: tekst z bundla
		directoryChooser.setTitle("This is my directory chooser");
		directoryChooser.setInitialDirectory(filePath);
		imageView.setImage(null);
		// Show open file dialog
		// REV: okno powinno byc modalne
		File file = directoryChooser.showDialog(null);
		if (file != null) {
			nameField.setText(file.getPath());
			imageFiles.setAll(load(file.toPath()));
			listView.setItems(imageFiles);
		}
		LOG.debug("Leaving OpenButonClick()");
	}

	// REV: lepiej podpiac changelistener pod selectedItem
	@FXML
	public void handleMouseClick(MouseEvent arg0) {
		LOG.debug("Entering SelectMouseClick()");
		if (!listView.getItems().isEmpty()) {
			String pathToImage = nameField.getText() + "\\" + listView.getSelectionModel().getSelectedItem().toString();
			// REV: zawsze uzywaj loggera
			System.out.println("Clicked on " + pathToImage);
			File file = new File(pathToImage);
			Image image = new Image(file.toURI().toString());
			imageView.setImage(image);
			imageView.setFitHeight(0);
			imageView.setFitWidth(0);
			imageView.setPreserveRatio(true);
		} else {
			LOG.debug("File list is empty...");
		}
		LOG.debug("Leaving SelectMouseClick()");
	}

	@FXML
	public void handleNextButtonClick(MouseEvent arg0) {
		LOG.debug("Entering NextButtonClick()");
		listView.getSelectionModel().selectNext();
		handleMouseClick(arg0);
		LOG.debug("Leaving NextButtonClick()");
	}

	@FXML
	public void handlePrevButtonClick(MouseEvent arg0) {
		LOG.debug("Entering PrevButtonClick()");
		listView.getSelectionModel().selectPrevious();
		handleMouseClick(arg0);
		LOG.debug("Leaving PrevButtonClick()");
	}

	@FXML
	public void handleSlideshowButtonClick(MouseEvent arg0) throws InterruptedException {
		LOG.debug("Entering SlideshowButtonClick()");
		Runnable backgroundTask = new Runnable() {

			@Override
			public void run() {
				for (int item = 0; item < listView.getItems().size(); item++) {
					listView.getSelectionModel().select(item);
					handleMouseClick(arg0);
					try {
						Thread.sleep(CALL_DELAY);
					} catch (InterruptedException e) {
						// REV: zawsze uzywaj loggera
						e.printStackTrace();
					}

					if (stopSlideShow) {
						return;
					}
				}
			}
		};
		new Thread(backgroundTask).start();
		LOG.debug("Leaving SlideshowButtonClick()");
	}

	@FXML
	public void handleStopSlideshowButtonClick(MouseEvent arg0) {
		LOG.debug("Entering StopSlideshowButtonClick()");
		if (arg0.getClickCount() == 1) {
			LOG.debug("Slideshow canceled...");
			// REV: czy da sie odpalic slideshow drugi raz?
			this.stopSlideShow = true;

		}
		LOG.debug("Leaving StopSlideshowButtonClick()");
	}

	public void handleZoomScroll(MouseEvent arg0) throws IllegalArgumentException {
		LOG.debug("Entering ZoomScroll()");
		// REV: dlaczego InvalidationListener?
		zoomProperty.addListener(new InvalidationListener() {
			@Override
			public void invalidated(Observable arg0) {
				imageView.setFitWidth(zoomProperty.get() * MAX_SCALE);
				imageView.setFitHeight(zoomProperty.get() * MIN_SCALE);
			}
		});

		// REV: dlaczego EventFilter?
		scrollPane.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if (event.getDeltaY() > 0) {
					zoomProperty.set(zoomProperty.get() * SCALE_FACTOR);
				} else if (event.getDeltaY() < 0) {
					zoomProperty.set(zoomProperty.get() / SCALE_FACTOR);
				}
			}
		});
		LOG.debug("Leaving ZoomScroll()");
	}

	private List<Path> load(Path directory) {
		List<Path> files = new ArrayList<>();
		try {
			Files.newDirectoryStream(directory, "*.{jpg,jpeg,png,gif,JPG,JPEG,PNG,GIF}")
					.forEach(file -> files.add(file.getFileName()));
		} catch (IOException e) {
			// REV: j.w.
			e.printStackTrace();
		}
		return files;
	}

};
