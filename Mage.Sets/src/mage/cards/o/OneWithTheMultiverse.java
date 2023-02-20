package mage.cards.o;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mage.MageIdentifier;
import mage.MageObjectReference;
import mage.abilities.Ability;
import mage.abilities.common.SimpleStaticAbility;
import mage.abilities.effects.AsThoughEffectImpl;
import mage.abilities.effects.common.continuous.LookAtTopCardOfLibraryAnyTimeEffect;
import mage.cards.Card;
import mage.cards.CardImpl;
import mage.cards.CardSetInfo;
import mage.constants.AsThoughEffectType;
import mage.constants.CardType;
import mage.constants.Duration;
import mage.constants.Outcome;
import mage.constants.WatcherScope;
import mage.constants.Zone;
import mage.game.Game;
import mage.game.events.GameEvent;
import mage.game.stack.Spell;
import mage.players.Player;
import mage.watchers.Watcher;

/**
 *
 * @author anonymous
 */
public final class OneWithTheMultiverse extends CardImpl {

    public OneWithTheMultiverse(UUID ownerId, CardSetInfo setInfo) {
        super(ownerId, setInfo, new CardType[]{CardType.ENCHANTMENT}, "{6}{U}{U}");
        
        Watcher watcher = new OneWithTheMultiverseWatcher();

        // You may look at the top card of your library any time.
        this.addAbility(new SimpleStaticAbility(new LookAtTopCardOfLibraryAnyTimeEffect()));

        // You may play lands and cast spells from the top of your library.

        Ability ability = new SimpleStaticAbility(Zone.BATTLEFIELD, new OneWithTheMultiversePlayTheTopCardEffect()).setIdentifier(MageIdentifier.OneWithTheMultiverseWatcher);

        this.addAbility(ability, watcher);

        // Once during each of your turns, you may cast a spell from your hand or the top of your library without paying its mana cost.
    }

    private OneWithTheMultiverse(final OneWithTheMultiverse card) {
        super(card);
    }

    @Override
    public OneWithTheMultiverse copy() {
        return new OneWithTheMultiverse(this);
    }
}

class OneWithTheMultiversePlayTheTopCardEffect extends AsThoughEffectImpl {

    OneWithTheMultiversePlayTheTopCardEffect() {
        super(AsThoughEffectType.PLAY_FROM_NOT_OWN_HAND_ZONE,
                Duration.WhileOnBattlefield, Outcome.Benefit, true);
        staticText = "You may play lands and cast spells from the top of your library.";
    }

    private OneWithTheMultiversePlayTheTopCardEffect(final OneWithTheMultiversePlayTheTopCardEffect effect) {
        super(effect);
    }

    @Override
    public boolean apply(Game game, Ability source) {
        return true;
    }

    @Override
    public OneWithTheMultiversePlayTheTopCardEffect copy() {
        return new OneWithTheMultiversePlayTheTopCardEffect(this);
    }

    @Override
    public boolean applies(UUID objectId, Ability source, UUID affectedControllerId, Game game) {
        // current card's part
        Card cardToCheck = game.getCard(objectId);
        if (cardToCheck == null) {
            return false;
        }

        // must be you
        if (!affectedControllerId.equals(source.getControllerId())) {
            return false;
        }

        // must be your card
        Player player = game.getPlayer(cardToCheck.getOwnerId());
        if (player == null || !player.getId().equals(affectedControllerId)) {
            return false;
        }

        // must be from your library
        Card topCard = player.getLibrary().getFromTop(game);
        if (topCard == null || !topCard.getId().equals(cardToCheck.getMainCard().getId())) {
            return false;
        }

        // allows to play/cast with alternative cost
        if (!cardToCheck.isLand(game)) {

            OneWithTheMultiverseWatcher watcher = game.getState().getWatcher(OneWithTheMultiverseWatcher.class);

            // check if ability was used
            if (watcher != null && !watcher.isAbilityUsed() && player.chooseUse(Outcome.Benefit, "Cast " + cardToCheck.getName() + " without paying its mana cost?", source, game)) {
                System.out.println("SETTING CASTING COST TO NONE");
                player.setCastSourceIdWithAlternateMana(cardToCheck.getId(), null, null);
            }


        }
        return true;
    }
}

class OneWithTheMultiverseWatcher extends Watcher {

    private final Set<MageObjectReference> allowingObjects = new HashSet<>();
    private final Map<MageObjectReference, UUID> castSpells = new HashMap<>();

    OneWithTheMultiverseWatcher() {
        super(WatcherScope.GAME);
    }

    @Override
    public void watch(GameEvent event, Game game) {
        if (GameEvent.EventType.SPELL_CAST.equals(event.getType())
                && event.hasApprovingIdentifier(MageIdentifier.OneWithTheMultiverseWatcher)) {
            Spell spell = (Spell) game.getObject(event.getTargetId());
            if (spell != null) {
                allowingObjects.add(event.getAdditionalReference().getApprovingMageObjectReference());
                castSpells.put(new MageObjectReference(spell.getMainCard().getId(), game),
                        event.getAdditionalReference().getApprovingAbility().getSourceId());
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        allowingObjects.clear();
    }

    public boolean isAbilityUsed() {
        return !allowingObjects.isEmpty();
    }

    public UUID spellCastWasAllowedBy(MageObjectReference mor) {
        return castSpells.getOrDefault(mor, null);
    }

}
