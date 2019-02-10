package ru.ibakaidov.distypepro.utils;

import java.util.ArrayList;
import java.util.Iterator;

import ru.ibakaidov.distypepro.DatabaseManager;
import ru.ibakaidov.distypepro.controllers.CategoriesController;
import ru.ibakaidov.distypepro.controllers.StatementsController;
import ru.ibakaidov.distypepro.structures.Category;
import ru.ibakaidov.distypepro.structures.Statement;

public class ImportManager {

    public static void importData(DatabaseManager databaseManager, CategoriesController categoriesController, StatementsController statementsController){
        String[] categories = databaseManager.getCategories().toArray(new String[0]);
        for (int i = 0; i < categories.length; i++) {
                String label = categories[i];
            Category category = new Category(label);
            categoriesController.pushToTable(category);
            String[] slabels = databaseManager.getStatements(i + 1).toArray(new String[0]);
            for (String slabel :
                    slabels) {
                Statement statement = new Statement(slabel, category.id);
                statementsController.pushToTable(statement);
            }
        }
    }

}
