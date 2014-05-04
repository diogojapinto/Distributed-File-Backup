package sdis.sharedbackup.frontend;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class GUI extends Application {

	public static void main(String[] args) 
	{
		launch(args);
	}

	/*@Override
	public void start(Stage primaryStage) throws Exception 
	{
		primaryStage.setTitle("Hello World!");
		Button btn = new Button();
		btn.setText("Say 'Hello World'");
		btn.setOnAction(new EventHandler<ActionEvent>() 
			{
				@Override
				public void handle(ActionEvent event) 
				{
					System.out.println("Hello World!");	
				}
			});

		StackPane root = new StackPane();
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 300, 250));
        primaryStage.show();
	}*/

	/*@Override
	public void start(Stage primaryStage) throws Exception 
	{
		primaryStage.setTitle("Welcome");

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 275);
		primaryStage.setScene(scene);
		scene.getStylesheets().add
		 (teste.class.getResource("Login.css").toExternalForm());

		Text scenetitle = new Text("Welcome");
		scenetitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		grid.add(scenetitle, 1, 0, 2, 1);

		Label userName = new Label("User Name:");
		grid.add(userName, 0, 2);

		TextField userTextField = new TextField();
		grid.add(userTextField, 1, 2);

		Label pw = new Label("Password:");
		grid.add(pw, 0, 3);

		PasswordField pwBox = new PasswordField();
		grid.add(pwBox, 1, 3);		

		Button btn = new Button("Sign in");
		HBox hbBtn = new HBox(10);
		hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
		hbBtn.getChildren().add(btn);
		grid.add(hbBtn, 1, 5);

		final Text actiontarget = new Text();
        grid.add(actiontarget, 1, 7);
    	btn.setOnAction(new EventHandler<ActionEvent>() {

		    @Override
		    public void handle(ActionEvent e) {
		        actiontarget.setFill(Color.FIREBRICK);
		        actiontarget.setText("Sign in button pressed");
		    }
		});

    	//grid.setGridLinesVisible(true);
		primaryStage.show();		
	}*/


	@Override
	public void start(final Stage primaryStage) throws Exception 
	{
		//primaryStage.setTitle("Welcome");

		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(GUI.class.getResource("Login.css").toExternalForm());

		//Elementos da cena

		Text welcome = new Text("Welcome User!");
		welcome.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));		

		Label option = new Label("Choose an option:");

		Button backup = new Button("Backup File");
		Button restore = new Button("Restore File");
		Button delete = new Button(" Delete a replicated File");
		Button space = new Button("Change allocated space");

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

		grid.add(hbWelcome, 0, 0);
		grid.add(hbOption, 0, 2);
		grid.add(hbBackupRestore, 0, 3);
		grid.add(hbRestore, 0, 4);
		grid.add(hbDelete, 0, 5);

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

		//grid.setGridLinesVisible(true);
		primaryStage.show();		
	}

	private void backupFile(final Stage primaryStage)
	{
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(GUI.class.getResource("Login.css").toExternalForm());

		//Elementos da cena

		Text fileBackup = new Text("File Backup");
		fileBackup.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label userName = new Label("File Path:"); 
		Label repDegree = new Label("Replication degree:");

		TextField userTextField = new TextField();
		TextField repTextField = new TextField();

		Button backup = new Button("Backup");
		Button cancel = new Button(" Cancel ");

		final Text actiontarget = new Text();
		grid.add(actiontarget, 0, 5);

		HBox fB = new HBox(10);
		fB.setAlignment(Pos.CENTER);
		fB.getChildren().add(fileBackup);

		HBox hbButtons = new HBox(10);
		hbButtons.setAlignment(Pos.BASELINE_CENTER);
		hbButtons.getChildren().add(backup);
		hbButtons.getChildren().add(cancel);

		grid.add(fB, 0, 0);				
		grid.add(userName, 0, 1);		
		grid.add(userTextField, 0, 2);		
		grid.add(repDegree, 0, 3);		
		grid.add(repTextField, 0, 4);
		grid.add(hbButtons, 0, 6);

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
		//grid.setGridLinesVisible(true);
	}

	private void restoreFile(final Stage primaryStage)
	{
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(GUI.class.getResource("Login.css").toExternalForm());

		//Elementos da cena

		Text fileRestore = new Text("File Restore");
		fileRestore.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label restore = new Label("Chose file to restore:"); 

		Button cancel = new Button(" Cancel ");

		final Text actiontarget = new Text();
		grid.add(actiontarget, 0, 5);

		HBox fR = new HBox(10);
		fR.setAlignment(Pos.CENTER);
		fR.getChildren().add(fileRestore);

		HBox hbCancel = new HBox(10);
		hbCancel.setAlignment(Pos.CENTER);
		hbCancel.getChildren().add(cancel);

		grid.add(fR, 0, 0);				
		grid.add(restore, 0, 1);		
		grid.add(hbCancel, 0, 3); //pos vai depender do numero de ficheiros

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
		//grid.setGridLinesVisible(true);
	}

	private void deleteFile(final Stage primaryStage)
	{
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.TOP_CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(GUI.class.getResource("Login.css").toExternalForm());

		//Elementos da cena

		Text fileDelete = new Text("File Delete");
		fileDelete.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));

		Label delete = new Label("Chose file to delete:"); 

		Button cancel = new Button(" Cancel ");

		final Text actiontarget = new Text();
		grid.add(actiontarget, 0, 5);

		HBox fD = new HBox(10);
		fD.setAlignment(Pos.CENTER);
		fD.getChildren().add(fileDelete);

		HBox hbCancel = new HBox(10);
		hbCancel.setAlignment(Pos.CENTER);
		hbCancel.getChildren().add(cancel);

		grid.add(fD, 0, 0);				
		grid.add(delete, 0, 1);		
		grid.add(hbCancel, 0, 3); //pos vai depender do numero de ficheiros

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
	
	private void spaceReclaim(final Stage primaryStage)
	{
		GridPane grid = new GridPane();
		grid.setAlignment(Pos.CENTER);
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(25, 25, 25, 25));

		Scene scene = new Scene(grid, 300, 280);
		primaryStage.setScene(scene);
		scene.getStylesheets().add(GUI.class.getResource("Login.css").toExternalForm());
		
		//Elementos da cena
		
		Text spaceReclaim = new Text("Space Reclaiming");
		spaceReclaim.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
		
		Label reclaim = new Label("Enter new allocated space:");
		
		TextField allocatedSpace = new TextField();
		
		Button set = new Button("Set space");
		Button cancel = new Button("Cancel");
		
		HBox sR = new HBox(10);
		sR.setAlignment(Pos.BASELINE_CENTER);
		sR.getChildren().add(spaceReclaim);
		
		HBox hbCancel = new HBox(10);
		hbCancel.setAlignment(Pos.CENTER);
		hbCancel.getChildren().add(cancel);
		
		HBox hbSet = new HBox(10);
		hbSet.setAlignment(Pos.CENTER);
		hbSet.getChildren().add(set);
		
		grid.add(sR, 0, 0);
		grid.add(reclaim, 0, 1);
		grid.add(allocatedSpace, 0, 2);
		grid.add(hbSet, 0, 3);
		grid.add(hbCancel, 0, 4);
		
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
