// TODO: verify if folder is valid

package sdis.sharedbackup.frontend;

import sdis.sharedbackup.backend.ConfigsManager;
import sdis.sharedbackup.backend.ConfigsManager.ConfigurationsNotInitializedException;
import sdis.sharedbackup.backend.ConfigsManager.FileAlreadySaved;
import sdis.sharedbackup.backend.ConfigsManager.InvalidBackupSizeException;
import sdis.sharedbackup.backend.ConfigsManager.InvalidFolderException;
import sdis.sharedbackup.backend.SharedFile.FileDoesNotExistsExeption;
import sdis.sharedbackup.backend.SharedFile.FileTooLargeException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Popup;

public class GUI extends Application {

	private File file;
	public static boolean backupSuccess = false;

	public static void main(String[] args) {
		ConfigsManager.getInstance()
				.setMulticastAddrs("239.0.0.1", Integer.parseInt("8765"),
						"239.0.0.1", Integer.parseInt("8766"), "239.0.0.1",
						Integer.parseInt("8767"));
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws Exception {
		primaryStage.setTitle("MFCSS");
		if (!ApplicationInterface.getInstance().getDatabaseStatus())
			setupService(primaryStage);
		else
			menu(primaryStage);

	}

	// TODO
	private void setupService(final Stage primaryStage) {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text setup = new Text("Setup");
		setup.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label labelSpace = new Label("Choose your allocated space (KB):");

		final TextField spaceTextField = new TextField();

		Button btnDirChooser = new Button("Choose folder to save files");
		final DirectoryChooser dirChooser = new DirectoryChooser();

		Button btnContinue = new Button("Continue");

		final Text chosenFile = new Text();
		chosenFile.setWrappingWidth(215);

		final Text errorMsg = new Text();

		HBox hbSetup = new HBox(10);
		hbSetup.setAlignment(Pos.TOP_CENTER);
		hbSetup.getChildren().add(setup);

		HBox hbBtnContinue = new HBox(10);
		hbBtnContinue.setAlignment(Pos.CENTER);
		hbBtnContinue.getChildren().add(btnContinue);

		HBox hbErrorMsg = new HBox(10);
		hbErrorMsg.setAlignment(Pos.BASELINE_CENTER);
		hbErrorMsg.getChildren().add(errorMsg);

		grid.add(hbSetup, 0, 1);
		grid.add(labelSpace, 0, 3);
		grid.add(spaceTextField, 0, 4);
		grid.add(btnDirChooser, 0, 5);
		grid.add(chosenFile, 0, 6);
		grid.add(hbErrorMsg, 0, 8);
		grid.add(hbBtnContinue, 0, 9);

		btnDirChooser.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				File file_temp = dirChooser.showDialog(primaryStage);

				if (file_temp != null) {
					file = file_temp;
					chosenFile.setText(file.getAbsolutePath());
				}
			}
		});

		btnContinue.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {

				if (file == null || spaceTextField.getText().equals("")) {
					errorMsg.setFill(Color.FIREBRICK);
					errorMsg.setText("Please fill all fields");
				} else if (!spaceTextField.getText().matches("\\d+")) // not an
																		// integer
				{
					errorMsg.setFill(Color.FIREBRICK);
					errorMsg.setText("Space not valid");
				} else {
					int space = Integer.parseInt(spaceTextField.getText());
					String path = file.getAbsolutePath();

					try {
						ApplicationInterface.getInstance()
								.setAvailableDiskSpace(space);
						ApplicationInterface.getInstance()
								.setDestinationDirectory(path);
					} catch (InvalidBackupSizeException e) {
						errorMsg.setFill(Color.FIREBRICK);
						errorMsg.setText("Invalid folder. Please select a empty folder");
					} catch (InvalidFolderException e) {
						errorMsg.setFill(Color.FIREBRICK);
						errorMsg.setText("Invalid size. Please input a positive integer");
					}
					menu(primaryStage);
				}
			}
		});
		primaryStage.show();
	}

	private void setArgs(final Stage primaryStage) {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(5);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena
		Text defineArgs = new Text("Network Settings");
		defineArgs.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label mcAdress = new Label("Control Channel Adress:");
		Label mdbAdress = new Label("Backup Channel Adress:");
		Label mdrAdress = new Label("Recovery Channel Adress:");

		final TextField mcAdressTextField = new TextField();
		final TextField mdbAdressTextField = new TextField();
		final TextField mdrAdressTextField = new TextField();
		
		// set fields to default addresses
		mcAdressTextField.setText(ConfigsManager.getInstance().getMCAddr().getHostAddress() + ":" + ConfigsManager.getInstance().getMCPort());
		mcAdressTextField.setText(ConfigsManager.getInstance().getMDBAddr().getHostAddress() + ":" + ConfigsManager.getInstance().getMDBPort());
		mcAdressTextField.setText(ConfigsManager.getInstance().getMDRAddr().getHostAddress() + ":" + ConfigsManager.getInstance().getMDRPort());

		final Text errorMsg = new Text();
		errorMsg.setFill(Color.FIREBRICK);

		Button change = new Button("Change");
		Button cancel = new Button(" Cancel ");
		// cancel.setMinWidth(60);

		HBox hbErrorMsg = new HBox(10);
		hbErrorMsg.setAlignment(Pos.BASELINE_CENTER);
		hbErrorMsg.getChildren().add(errorMsg);

		HBox hbButtons = new HBox(10);
		hbButtons.setAlignment(Pos.BASELINE_CENTER);
		hbButtons.getChildren().add(change);
		hbButtons.getChildren().add(cancel);

		grid.add(defineArgs, 0, 0);
		grid.add(mcAdress, 0, 2);
		grid.add(mcAdressTextField, 0, 3);
		grid.add(mdbAdress, 0, 4);
		grid.add(mdbAdressTextField, 0, 5);
		grid.add(mdrAdress, 0, 6);
		grid.add(mdrAdressTextField, 0, 7);
		grid.add(hbErrorMsg, 0, 8);
		grid.add(hbButtons, 0, 9);

		change.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {

				if (mcAdressTextField.getText().equals("")
						|| mdbAdressTextField.getText().equals("")
						|| mdrAdressTextField.getText().equals("")) {
					errorMsg.setText("Please fill all fields");
				} else {
					String splitmc[] = mcAdressTextField.getText().split(":"), splitmdb[] = mdbAdressTextField
							.getText().split(":"), splitmdr[] = mdrAdressTextField
							.getText().split(":");
					String mcAdr = splitmc[0], mdbAdr = splitmdb[0], mdrAdr = splitmdr[0];

					if (!splitmc[1].matches("\\d+")
							|| !splitmdb[1].matches("\\d+")
							|| !splitmdr[1].matches("\\d+")) // port not an
																// integer
					{
						errorMsg.setText("Invalid port(s)");
					} else {
						int mcPort = Integer.parseInt(splitmc[1]), mdbPort = Integer
								.parseInt(splitmdb[1]), mdrPort = Integer
								.parseInt(splitmdr[1]);

						ConfigsManager.getInstance().setMulticastAddrs(mcAdr,
								mcPort, mdbAdr, mdbPort, mdrAdr, mdrPort);
						
						try {
							ConfigsManager.getInstance().init();
						} catch (ConfigurationsNotInitializedException e1) {
							e1.printStackTrace();
						}

					}
				}
			}
		});

		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				menu(primaryStage);
			}
		});

		primaryStage.show();
	}

	private void menu(final Stage primaryStage) {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text welcome = new Text("Welcome User!");
		welcome.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label option = new Label("Choose an option:");

		Button backup = new Button("Backup File");
		Button restore = new Button("Restore File");
		Button delete = new Button(" Delete a replicated File");
		Button space = new Button("Change allocated space");
		Button settings = new Button("Change network settings");

		delete.setMinWidth(165);
		space.setMinWidth(165);

		HBox hbWelcome = new HBox(10);
		hbWelcome.setAlignment(Pos.BASELINE_CENTER);
		hbWelcome.getChildren().add(welcome);

		HBox hbOption = new HBox(10);
		hbOption.setAlignment(Pos.BASELINE_CENTER);
		hbOption.getChildren().add(option);

		HBox hbBackupRestore = new HBox(10);
		hbBackupRestore.setAlignment(Pos.BASELINE_CENTER);
		hbBackupRestore.getChildren().add(backup);
		hbBackupRestore.getChildren().add(restore);

		HBox hbRestore = new HBox(10);
		hbRestore.setAlignment(Pos.BASELINE_CENTER);
		hbRestore.getChildren().add(delete);

		HBox hbDelete = new HBox(10);
		hbDelete.setAlignment(Pos.BASELINE_CENTER);
		hbDelete.getChildren().add(space);

		HBox hbSettings = new HBox(10);
		hbSettings.setAlignment(Pos.BASELINE_CENTER);
		hbSettings.getChildren().add(settings);

		grid.add(hbWelcome, 0, 0);
		grid.add(hbOption, 0, 2);
		grid.add(hbBackupRestore, 0, 3);
		grid.add(hbRestore, 0, 4);
		grid.add(hbDelete, 0, 5);
		grid.add(hbSettings, 0, 7);

		backup.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				backupFile(primaryStage);
			}
		});

		restore.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				restoreFile(primaryStage);
			}
		});

		delete.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				deleteFile(primaryStage);
			}
		});

		space.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				spaceReclaim(primaryStage);
			}
		});

		settings.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				setArgs(primaryStage);
			}
		});

		// grid.setGridLinesVisible(true);
		primaryStage.show();
	}

	private void backupFile(final Stage primaryStage) {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text fileBackup = new Text("File Backup");

		Label repDegree = new Label("Replication degree:");

		final TextField repTextField = new TextField();

		Button backup = new Button("Backup");
		Button cancel = new Button(" Cancel ");
		Button chooseFile = new Button("Choose a file");

		final FileChooser fileChooser = new FileChooser();

		final Text chosenFile = new Text();
		final Text errorMsg = new Text();

		fileBackup.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		errorMsg.setFill(Color.FIREBRICK);
		repTextField.setMaxWidth(110);
		chosenFile.setWrappingWidth(215);
		errorMsg.setTextAlignment(TextAlignment.CENTER);

		HBox fB = new HBox(10);
		fB.setAlignment(Pos.CENTER);
		fB.getChildren().add(fileBackup);

		HBox hbChooseFile = new HBox(10);
		hbChooseFile.setAlignment(Pos.BASELINE_LEFT);
		hbChooseFile.getChildren().add(chooseFile);

		HBox hbChosenFile = new HBox(10);
		hbChosenFile.setAlignment(Pos.BASELINE_LEFT);
		hbChosenFile.setMaxWidth(50);
		hbChosenFile.getChildren().add(chosenFile);

		HBox hbErrorMsg = new HBox(10);
		hbErrorMsg.setAlignment(Pos.BASELINE_CENTER);
		hbErrorMsg.getChildren().add(errorMsg);

		HBox hbButtons = new HBox(10);
		hbButtons.setAlignment(Pos.BASELINE_CENTER);
		hbButtons.getChildren().add(backup);
		hbButtons.getChildren().add(cancel);

		grid.add(fB, 0, 0);
		grid.add(repDegree, 0, 2);
		grid.add(repTextField, 0, 3);
		grid.add(hbChooseFile, 0, 4);
		grid.add(hbChosenFile, 0, 5);
		grid.add(hbErrorMsg, 0, 6);
		grid.add(hbButtons, 0, 7);

		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					start(primaryStage);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

		chooseFile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(final ActionEvent e) {

				File file_temp = fileChooser.showOpenDialog(primaryStage);

				if (file_temp != null) {
					file = file_temp;
					chosenFile.setText(file.getAbsolutePath());
				}
			}
		});

		backup.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {

				final String repDegree = repTextField.getText();

				if (file == null || repDegree.equals("")) {
					errorMsg.setText("Please fill all fields");
				} else if (!repDegree.matches("\\d+")) // not an integer
				{
					errorMsg.setText("Replication degree not valid");
				} else {
					errorMsg.setText("Backing up file");

					Thread t = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								ApplicationInterface.getInstance().backupFile(
										file.getAbsolutePath(),
										Integer.parseInt(repDegree));
							} catch (FileTooLargeException e1) {
								System.out
										.println("The selected file is too large");
							} catch (FileDoesNotExistsExeption e1) {
								System.out
										.println("The selected file does not exists");
							} catch (FileAlreadySaved e1) {
								System.out
										.println("The selected file is already in the database");
							}

							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									if (!backupSuccess)
										errorMsg.setText("Not enough peers to meet\n necessary replication");
									else
										errorMsg.setText("File backed up");
								}
							});
						}
					});

					t.start();
				}
			}
		});
		// grid.setGridLinesVisible(true);
	}

	private void restoreFile(final Stage primaryStage) {
		ArrayList<String> restorableFiles = ApplicationInterface.getInstance()
				.getRestorableFiles();

		if (restorableFiles.size() == 0) {
			try {
				final Popup alertMessg = new Popup();
				alertMessg.setAutoFix(false);
				alertMessg.setHideOnEscape(true);

				Button btnGoBack = new Button("Ok");
				Label lblAlert = new Label(
						"You do not have any backed up files");

				btnGoBack.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {
						primaryStage.setOpacity(1);
						alertMessg.hide();
					}
				});

				VBox popUpBox = new VBox(10);
				popUpBox.setPadding(new Insets(15));

				popUpBox.getChildren().add(lblAlert);
				popUpBox.getChildren().add(btnGoBack);
				popUpBox.setAlignment(Pos.CENTER);

				alertMessg.getContent().add(popUpBox);

				primaryStage.setOpacity(0.5);
				alertMessg.show(primaryStage);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return;
		}

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text fileRestore = new Text("File Restore");
		fileRestore.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label restore = new Label("Chose file to restore:");

		ObservableList<String> filesAvailable = FXCollections
				.observableArrayList(restorableFiles);

		final ComboBox<String> selectableFiles = new ComboBox<String>(
				filesAvailable);
		selectableFiles.setMinWidth(200);

		Button btnRestore = new Button("Restore");
		Button btnCancel = new Button("Cancel");

		final Text errorMsg = new Text();

		HBox fR = new HBox(10);
		fR.setAlignment(Pos.CENTER);
		fR.getChildren().add(fileRestore);

		HBox hbBtns = new HBox(10);
		hbBtns.setAlignment(Pos.CENTER);
		hbBtns.getChildren().add(btnRestore);
		hbBtns.getChildren().add(btnCancel);

		grid.add(fR, 0, 0);
		grid.add(restore, 0, 1);
		grid.add(selectableFiles, 0, 2);
		grid.add(hbBtns, 0, 3); // pos vai depender do numero de ficheiros
		grid.add(errorMsg, 0, 5);

		btnRestore.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				String selectedFile = selectableFiles.getSelectionModel()
						.getSelectedItem();
				if (selectedFile == null) {
					errorMsg.setFill(Color.FIREBRICK);
					errorMsg.setText("Please select a valid file from the list above");
				} else {
					errorMsg.setFill(Color.GOLD);
					errorMsg.setText("Restoring file to selected path");
					if (!ApplicationInterface.getInstance().restoreFileByPath(
							selectedFile)) {
						errorMsg.setFill(Color.FIREBRICK);
						errorMsg.setText("Error restoring file");
					} else {
						errorMsg.setFill(Color.OLIVEDRAB);
						errorMsg.setText("Restore successfull");
					}
				}
			}
		});

		btnCancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					start(primaryStage);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		// grid.setGridLinesVisible(true);
	}

	private void deleteFile(final Stage primaryStage) {
		ArrayList<String> deletableFiles = ApplicationInterface.getInstance()
				.getDeletableFiles();

		if (deletableFiles.size() == 0) {
			try {
				final Popup alertMessg = new Popup();
				alertMessg.setAutoFix(false);
				alertMessg.setHideOnEscape(true);

				Button btnGoBack = new Button("Ok");
				Label lblAlert = new Label(
						"You do not have any backed up files");

				btnGoBack.setOnAction(new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent arg0) {
						primaryStage.setOpacity(1);
						alertMessg.hide();
					}
				});

				VBox popUpBox = new VBox(10);
				popUpBox.setPadding(new Insets(15));

				popUpBox.getChildren().add(lblAlert);
				popUpBox.getChildren().add(btnGoBack);
				popUpBox.setAlignment(Pos.CENTER);

				alertMessg.getContent().add(popUpBox);

				primaryStage.setOpacity(0.5);
				alertMessg.show(primaryStage);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return;
		}

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text fileDeletion = new Text("File Deletion");
		fileDeletion.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label restore = new Label("Chose file to restore:");

		ObservableList<String> filesAvailable = FXCollections
				.observableArrayList(deletableFiles);

		final ComboBox<String> selectableFiles = new ComboBox<String>(
				filesAvailable);
		selectableFiles.setMinWidth(200);

		Button btnDelete = new Button("Delete");
		Button btnCancel = new Button("Cancel");

		final Text errorMsg = new Text();

		HBox hbDF = new HBox(10);
		hbDF.setAlignment(Pos.CENTER);
		hbDF.getChildren().add(fileDeletion);

		HBox hbBtns = new HBox(10);
		hbBtns.setAlignment(Pos.CENTER);
		hbBtns.getChildren().add(btnDelete);
		hbBtns.getChildren().add(btnCancel);

		grid.add(hbDF, 0, 0);
		grid.add(restore, 0, 1);
		grid.add(selectableFiles, 0, 2);
		grid.add(hbBtns, 0, 3); // pos vai depender do numero de ficheiros
		grid.add(errorMsg, 0, 5);

		btnDelete.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				String selectedFile = selectableFiles.getSelectionModel()
						.getSelectedItem();
				if (selectedFile == null) {
					errorMsg.setFill(Color.FIREBRICK);
					errorMsg.setText("Please select a valid file from the list above");
				} else {
					errorMsg.setFill(Color.GOLD);
					errorMsg.setText("Deleting file from service");
					try {
						if (!ApplicationInterface.getInstance().deleteFile(
								selectedFile)) {
							errorMsg.setFill(Color.FIREBRICK);
							errorMsg.setText("Error restoring file");
						} else {
							errorMsg.setFill(Color.OLIVEDRAB);
							errorMsg.setText("Restore successfull");
						}
					} catch (FileDoesNotExistsExeption e) {
						e.printStackTrace();
					}
				}
			}
		});

		btnCancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					start(primaryStage);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		// grid.setGridLinesVisible(true);
	}

	private void spaceReclaim(final Stage primaryStage) {
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(
				GUI.class.getResource("Login.css").toExternalForm());

		// Elementos da cena

		Text spaceReclaim = new Text("Space Reclaiming");
		spaceReclaim.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label reclaim = new Label("Enter new allocated space:");

		TextField allocatedSpace = new TextField();

		Button set = new Button("Set space");
		Button cancel = new Button("Cancel");

		HBox sR = new HBox(10);
		sR.setAlignment(Pos.BASELINE_CENTER);
		sR.getChildren().add(spaceReclaim);

		HBox hbButtons = new HBox(10);
		hbButtons.setAlignment(Pos.CENTER);
		hbButtons.getChildren().add(set);
		hbButtons.getChildren().add(cancel);

		grid.add(sR, 0, 0);
		grid.add(reclaim, 0, 1);
		grid.add(allocatedSpace, 0, 2);
		grid.add(hbButtons, 0, 3);

		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				try {
					start(primaryStage);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
	}
}
