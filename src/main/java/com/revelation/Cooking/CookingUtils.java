package com.revelation.Cooking;

import com.wurmonline.server.behaviours.MethodsItems;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Ingredient;
import com.wurmonline.server.items.IngredientGroup;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Recipe;

import java.util.*;

public class CookingUtils {
    private static void moveIngredient(Creature performer, Ingredient ingredient, Item container, Boolean multiple) {
        Set<Integer> seen = new HashSet<>();
        for (Item potential : container.getAllItems(false)) {
            if (ingredient.matches(potential)) {
                if (multiple)
                    seen.add(potential.getTemplateId());
                else
                    return;
            }
        }

        for (Item potential : performer.getInventory().getAllItems(false)) {
            try {
                if (ingredient.matches(potential) && !seen.contains(potential.getTemplateId())) {
                    if (container.itemCanBeInserted(potential)) {
                        if (potential.moveToItem(performer, container.getWurmId(), true)) {
                            performer.getCommunicator().sendUpdateInventoryItem(potential);
                            if (multiple) {
                                seen.add(potential.getTemplateId());
                            } else {
                                break;
                            }
                        }

                    }
                }
            } catch (Exception e) {
                CookingMod.logException(String.format("Error when moving recipe item=%d (%s), target=%d (%s) for player %s", potential.getWurmId(), potential.getName(), container.getWurmId(), container.getName(), performer.getName()), e);
            }
        }
    }

    public static void moveIngredients(Creature performer, Recipe recipe, Item container) {
        List<Ingredient> liquids = new ArrayList<>();

        for (IngredientGroup group : recipe.getGroups()) {
            boolean multiple = group.getGroupType() == 4 || group.getGroupType() == 6;
            List<Ingredient> ingredients = new ArrayList<>();
            for (Ingredient ingredient : group.getIngredients()) {
                if (ingredient.isLiquid())
                    liquids.add(ingredient);
                else
                    moveIngredient(performer, ingredient, container, multiple);

            }
        }

        int solids = Arrays.stream(container.getAllItems(false)).filter(x -> !x.isLiquid()).mapToInt(Item::getWeightGrams).sum();

        for (Ingredient ingredient : liquids) {
            int haveWeight = Arrays.stream(container.getAllItems(false)).filter(ingredient::matches).mapToInt(Item::getWeightGrams).sum();
            int neededWeight = Math.min((int) (solids * ingredient.getRatio() / 100 * 0.8) - haveWeight, container.getFreeVolume());
            if (neededWeight > 0) {
                for (Item potential : performer.getInventory().getAllItems(false)) {
                    try {
                        if (ingredient.matches(potential)) {
                            if (potential.getWeightGrams() > neededWeight) {
                                Item newItem = MethodsItems.splitLiquid(potential, neededWeight, performer);
                                if (container.insertItem(newItem)) {
                                    break;
                                }
                            } else {
                                if (potential.moveToItem(performer, container.getWurmId(), true)) {
                                    performer.getCommunicator().sendUpdateInventoryItem(potential);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        CookingMod.logException(String.format("Error when moving recipe item=%d (%s), target=%d (%s) for player %s", potential.getWurmId(), potential.getName(), container.getWurmId(), container.getName(), performer.getName()), e);
                    }
                }
            }
        }

    }
}