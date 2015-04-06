package game;

public class Controller {

	Model  model;
    View   view;
    
    Controller( Model model ){
        this.model = model;
        this.view = new View(model);
        model.addObserver(view);
    }//constructor Controller
}//Controller
