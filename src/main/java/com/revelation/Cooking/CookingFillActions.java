package com.revelation.Cooking;

import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.Recipe;
import com.wurmonline.server.items.Recipes;
import com.wurmonline.server.items.RecipesByPlayer;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.BehaviourProvider;
import org.gotti.wurmunlimited.modsupport.actions.ModActions;

import java.util.*;
import java.util.stream.Collectors;

public class CookingFillActions implements BehaviourProvider {
    private final Map<Integer, List<FillActionPerformer>> actions;

    public CookingFillActions() {
        actions = new HashMap<>();
        Arrays.stream(Recipes.getAllRecipes()).filter(Recipe::hasContainer).forEach(recipe -> {
            ActionEntry action = ActionEntry.createEntry((short) ModActions.getNextActionId(), recipe.getName(), "moving", new int[]{
                    0, //ACTION_TYPE_QUICK
                    37, //ACTION_TYPE_NEVER_USE_ACTIVE_ITEM
            });
            FillActionPerformer performer = new FillActionPerformer(action, recipe);
            ModActions.registerAction(action);
            ModActions.registerActionPerformer(performer);
            recipe.getContainerTemplates().forEach(template -> {
                if (!actions.containsKey(template.getTemplateId())) {
                    actions.put(template.getTemplateId(), new ArrayList<>());
                }
                actions.get(template.getTemplateId()).add(performer);
            });
        });
    }

    private boolean canUse(Creature performer, Item target) {
        return target != null && performer.isPlayer();
    }

    @Override
    public List<ActionEntry> getBehavioursFor(final Creature performer, final Item source, final Item target) {
        return getBehavioursFor(performer, target);
    }

    @Override
    public List<ActionEntry> getBehavioursFor(final Creature performer, final Item target) {
        if (canUse(performer, target) && actions.containsKey(target.getTemplateId())) {

            Map<String, List<FillActionPerformer>> performers = actions.get(target.getTemplateId()).stream()
                    .filter(x -> performer.getPower() > 3 || RecipesByPlayer.isKnownRecipe(performer.getWurmId(), x.recipe.getRecipeId()))
                    .collect(Collectors.groupingBy(x -> x.recipe.getSkillName()));

            ArrayList<ActionEntry> tmp = new ArrayList<>();

            int entries = performers.keySet().stream().sorted().mapToInt(skill -> {
                List<ActionEntry> recipes =
                        performers.get(skill).stream()
                                .sorted(Comparator.comparing(o -> o.recipe.getName().toLowerCase()))
                                .map(o -> o.action)
                                .collect(Collectors.toList());
                if (recipes.size() > 20) {
                    int total = (int) Math.ceil(recipes.size() / 20.0);
                    for (int i = 0; i < total; i++) {
                        List<ActionEntry> partial = recipes.subList(i * 20, Math.min((i + 1) * 20, recipes.size() - 1));
                        tmp.add(new ActionEntry((short) (-1 * partial.size()), String.format("%s (%d/%d)", skill, i + 1, total), ""));
                        tmp.addAll(partial);
                    }
                    return total;
                } else {
                    tmp.add(new ActionEntry((short) (-1 * recipes.size()), skill, ""));
                    tmp.addAll(recipes);
                    return 1;
                }
            }).sum();

            tmp.add(0, new ActionEntry((short) (-1 * entries), "Fill", ""));

            return tmp;
        } else return Collections.emptyList();
    }

    private class FillActionPerformer implements ActionPerformer {
        ActionEntry action;
        Recipe recipe;

        public FillActionPerformer(ActionEntry action, Recipe recipe) {
            this.action = action;
            this.recipe = recipe;
        }

        @Override
        public short getActionId() {
            return action.getNumber();
        }

        @Override
        public boolean action(final Action act, final Creature performer, final Item source, final Item target, final short action, final float counter) {
            return action(act, performer, target, action, counter);
        }

        @Override
        public boolean action(final Action act, final Creature performer, final Item target, final short action, final float counter) {
            CookingUtils.moveIngredients(performer, recipe, target);
            return true;
        }
    }
}
