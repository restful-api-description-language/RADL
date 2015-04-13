/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import radl.core.code.RadlCode;
import radl.core.validation.Issue.Level;


/**
 * Lint-inspired RADL validator that checks against best practices.
 */
public class LintValidator implements Validator {

  private final Map<String, String> resourcesByLocation = new HashMap<String, String>();
  private final Map<String, Collection<String>> checkedResourcesByResource = new HashMap<String, Collection<String>>();
  private final Collection<String> actions = new ArrayList<String>();
  private Collection<Issue> issues;
  private RadlCode radl;

  public LintValidator() {
    addActions("abide", "accelerate", "accept", "accomplish", "achieve", "acquire", "act", "activate", "adapt", "add",
        "administer", "admire", "admit", "adopt", "advise", "afford", "agree", "alight", "allow", "alter", "amuse",
        "analyze", "announce", "annoy", "anticipate", "apologize", "appear", "applaud", "apply", "appoint", "appraise",
        "appreciate", "approve", "arbitrate", "argue", "arise", "arrange", "arrest", "arrive", "ascertain", "ask",
        "assemble", "assess", "assure", "attach", "attain", "attend", "attract", "avoid", "awake", "bake", "bathe",
        "battle", "be", "become", "beg", "begin", "behave", "behold", "belong", "bend", "beset", "bind", "bite",
        "bleach", "bleed", "bless", "blink", "blot", "blow", "blush", "boast", "boil", "bore", "borrow", "bounce",
        "breathe", "breed", "brief", "bring", "build", "bump", "burn", "burst", "bury", "buy", "calculate", "call",
        "camp", "care", "carry", "carve", "chase", "cheat", "check", "cheer", "chew", "choke", "choose", "chop",
        "clap", "clarify", "classify", "clean", "clear", "cling", "clip", "close", "clothe", "coach", "coil",
        "collect", "comb", "come", "command", "communicate", "compare", "compete", "compile", "complain", "complete",
        "compose", "compute", "conceive", "concentrate", "conceptualize", "conclude", "confess", "confront", "confuse",
        "connect", "conserve", "consider", "consist", "consolidate", "construct", "contain", "continue", "control",
        "convert", "coordinate", "copy", "correct", "correlate", "cough", "counsel", "crawl", "create", "creep",
        "cross", "crush", "cry", "cut", "dare", "decay", "deceive", "decide", "decorate", "define", "delegate",
        "delight", "deliver", "demonstrate", "depend", "describe", "deserve", "destroy", "detect", "determine",
        "develop", "devise", "diagnose", "dig", "direct", "disagree", "disappear", "disapprove", "disarm", "discover",
        "dislike", "dispense", "display", "disprove", "dissect", "distribute", "dive", "divert", "divide", "do",
        "double", "drag", "drain", "dramatize", "draw", "dream", "drip", "drop", "drown", "drum", "dry", "dust",
        "dwell", "earn", "eat", "edit", "educate", "eliminate", "embarrass", "employ", "empty", "enact", "encourage",
        "endure", "enforce", "engineer", "enhance", "enjoy", "enlist", "ensure", "enter", "entertain", "escape",
        "establish", "evaluate", "examine", "exceed", "excite", "excuse", "execute", "exhibit", "exist", "expand",
        "expect", "expedite", "experiment", "explain", "explode", "express", "extend", "extract", "facilitate", "fade",
        "fail", "fancy", "fasten", "fax", "fear", "feed", "feel", "fetch", "fill", "finalize", "finance", "find",
        "fire", "fit", "fix", "flap", "flash", "flee", "fling", "float", "flood", "flow", "fly", "fold", "follow",
        "forbid", "forego", "foresee", "foretell", "forget", "forgive", "formulate", "forsake", "freeze", "frighten",
        "fry", "gather", "gaze", "generate", "get", "give", "glow", "glue", "go", "govern", "grab", "graduate",
        "grate", "grease", "greet", "grin", "grind", "grip", "groan", "grow", "guarantee", "guard", "handwrite",
        "hang", "happen", "harass", "harm", "hate", "haunt", "heal", "hear", "heat", "help", "hide", "hit", "hold",
        "hop", "hope", "hover", "hum", "hurry", "hurt", "hypothesize", "identify", "ignore", "illustrate", "imagine",
        "implement", "impress", "improve", "improvise", "include", "increase", "induce", "influence", "inform",
        "initiate", "inject", "injure", "inlay", "innovate", "inspect", "inspire", "install", "institute", "instruct",
        "insure", "integrate", "intend", "intensify", "interfere", "interlay", "interpret", "introduce", "invent",
        "investigate", "invite", "irritate", "itch", "jam", "jog", "join", "juggle", "justify", "keep", "kick", "kill",
        "kiss", "kneel", "knit", "knock", "know", "lay", "lead", "lean", "learn", "lecture", "led", "lend", "let",
        "lick", "lie", "lifted", "lighten", "listen", "live", "locate", "lose", "love", "maintain", "make", "manage",
        "manipulate", "manufacture", "march", "mark", "market", "marry", "matter", "mean", "meddle", "mediate", "meet",
        "melt", "memorize", "mend", "milk", "mine", "mislead", "miss", "misspell", "mistake", "misunderstand", "moan",
        "modify", "moor", "motivate", "mourn", "mow", "muddle", "multiply", "navigate", "negotiate", "nest", "nod",
        "nominate", "normalize", "obey", "observe", "obtain", "occur", "offend", "offer", "officiate", "open",
        "operate", "organize", "orient", "originate", "overcome", "overdo", "overdraw", "overflow", "overhear",
        "overtake", "overthrow", "owe", "own", "paddle", "paint", "participate", "paste", "pat", "pay", "peck", "peel",
        "peep", "perceive", "perfect", "perform", "permit", "persuade", "pinch", "pine", "pinpoint", "pioneer",
        "plead", "please", "plug", "possess", "pour", "praised", "pray", "preach", "precede", "predict", "prefer",
        "prepare", "prescribe", "preserve", "preset", "preside", "pretend", "prevent", "prick", "print", "procure",
        "produce", "profess", "promote", "proofread", "propose", "protect", "prove", "provide", "publicize", "punish",
        "purchase", "push", "put", "qualify", "quit", "race", "radiate", "rain", "raise", "reach", "read", "realign",
        "realize", "receive", "recognize", "recommend", "reconcile", "recruit", "reduce", "refer", "reflect", "refuse",
        "regulate", "rehabilitate", "reinforce", "reject", "rejoice", "relate", "relax", "remain", "remember",
        "remind", "remove", "render", "reorganize", "replace", "reply", "report", "represent", "reproduce", "research",
        "resolve", "respond", "restore", "restructure", "retire", "retrieve", "revise", "rhyme", "rid", "rinse",
        "rise", "rob", "rot", "rush", "sail", "satisfy", "save", "saw", "say", "scare", "scatter", "scold", "scorch",
        "scrape", "scratch", "scribble", "scrub", "secure", "see", "seek", "select", "sell", "send", "separate",
        "settle", "sew", "shave", "shear", "shed", "shelter", "shine", "shiver", "shock", "shoot", "shrink", "shrug",
        "shut", "simplify", "sing", "sink", "sip", "sit", "sketch", "ski", "skip", "slap", "slay", "sleep", "sling",
        "slink", "smash", "smite", "smoke", "snatch", "sneak", "sneeze", "snore", "snow", "soak", "solve", "soothe",
        "soothsay", "sort", "sow", "sparkle", "speak", "specify", "speed", "spell", "spend", "spill", "spit", "split",
        "spoil", "spray", "spread", "sprout", "squash", "squeak", "squeal", "squeeze", "stain", "stare", "start",
        "stay", "steal", "steer", "stick", "stimulate", "sting", "stink", "stir", "stitch", "stop", "streamline",
        "strengthen", "stretch", "stride", "strike", "string", "strip", "strive", "stroke", "structure", "stuff",
        "sublet", "subtract", "succeed", "suck", "suffer", "suggest", "summarize", "supervise", "support", "suppose",
        "surprise", "surround", "suspect", "suspend", "swear", "sweat", "sweep", "swell", "swim", "swing", "symbolize",
        "synthesize", "systemize", "tabulate", "take", "tame", "tap", "teach", "tear", "tease", "tell", "tempt",
        "terrify", "thank", "thaw", "think", "thrive", "throw", "thrust", "tickle", "tip", "tire", "touch", "tow",
        "transcribe", "transform", "translate", "transport", "trap", "travel", "tread", "tremble", "trick", "trot",
        "troubleshoot", "trust", "try", "tug", "tumble", "undergo", "understand", "undertake", "undress", "unfasten",
        "unify", "unite", "unlock", "unpack", "untidy", "update", "upgrade", "uphold", "upset", "use", "utilize",
        "vanish", "verbalize", "verify", "vex", "wail", "wait", "wake", "walk", "wander", "want", "warm", "warn",
        "wash", "waste", "watch", "wave", "wear", "weave", "wed", "weep", "weigh", "wend", "wet", "whine", "whip",
        "whirl", "wind", "wink", "wipe", "withhold", "withstand", "wobble", "worry", "wreck", "wrestle", "wriggle",
        "wring", "write", "yawn", "yell");
  }

  private void addActions(String... verbs) {
    for (String verb : verbs) {
      actions.add(verb);
    }
  }

  @Override
  public void validate(InputStream contents, Collection<Issue> messages) {
    radl = new RadlCode();
    radl.add(contents);
    issues = messages;
    resourcesByLocation.clear();
    validate();
  }

  private void validate() {
    validateStateDiagram();
    validateLinkRelations();
    validateMediaTypes();
    validateResourceModel();
  }

  private void validateLinkRelations() {
    Set<String> linkRelations = new TreeSet<String>();
    for (String linkRelation : radl.linkRelationNames()) {
      if (linkRelations.contains(linkRelation)) {
        error("Duplicate link-relation: '%s'", linkRelation);
      } else {
        linkRelations.add(linkRelation);
        validateLinkRelation(linkRelation);
      }
    }
  }

  private void validateLinkRelation(String linkRelation) {
    for (String transition : radl.linkRelationTransitions(linkRelation)) {
      assertKnownTransition(linkRelation, transition);
    }
  }

  private void assertKnownTransition(String linkRelation, String name) {
    for (String state : radl.stateNames()) {
      for (String transition : radl.stateTransitionNames(state)) {
        if (name.equals(transition)) {
          return;
        }
      }
    }
    warn("Link relation '%s' makes undefined transition '%s' discoverable", linkRelation, name);
  }

  private void validateMediaTypes() {
    Set<String> mediaTypes = new TreeSet<String>();
    for (String mediaType : radl.mediaTypeNames()) {
      if (mediaTypes.contains(mediaType)) {
        error("Duplicate media-type: '%s'", mediaType);
      } else {
        mediaTypes.add(mediaType);
      }
    }
  }

  private void validateResourceModel() {
    Set<String> resources = new TreeSet<String>();
    for (String resource : radl.resourceNames()) {
      if (resources.contains(resource)) {
        error("Duplicate resource: '%s'", resource);
      } else {
        resources.add(resource);
        validateResource(resource);
      }
    }
  }

  private void validateResource(String name) {
    validateLocation(name);
    validateMethods(name);
  }

  private void validateLocation(String name) {
    String location = radl.resourceLocation(name);
    if (location.isEmpty()) {
      warn("Resource '%s' has no location", name);
    } else {
      validateActionUri(name, location);
      validateDuplicateTemplateVariable(name, location);
      validateDuplicateParts(name, location);
      validateDuplicateLocation(name, location);
      validateFixedAndVariableLocation(name, location);
    }
  }

  private void validateActionUri(String name, String location) {
    if (containsAction(location)) {
      warn("Location of '%s' contains action: %s", name, location);
    }
  }

  private void validateDuplicateTemplateVariable(String name, String location) {
    String duplicateVariable = getDuplicateTemplateVariable(location);
    if (duplicateVariable != null) {
      warn("URI Template of '%s' contains duplicate variable '%s': %s", name, duplicateVariable, location);
    }
  }

  private String getDuplicateTemplateVariable(String uri) {
    Collection<String> variables = new ArrayList<String>();
    for (String part : splitUri(uri)) {
      String variable = getVariable(part);
      if (variable != null) {
        if (variables.contains(variable)) {
          return variable;
        }
        variables.add(variable);
      }
    }
    return null;
  }

  private String[] splitUri(String uri) {
    return uri.split("/");
  }

  private String getVariable(String uriPart) {
    return isVariable(uriPart) ? uriPart.substring(1, uriPart.length() - 1) : null;
  }

  private boolean isVariable(String uriPart) {
    return uriPart.startsWith("{") && uriPart.endsWith("}");
  }

  private void validateDuplicateParts(String name, String location) {
    Collection<String> parts = new ArrayList<String>();
    for (String part : splitUri(location)) {
      if (parts.contains(part)) {
        warn("Location of '%s' contains duplicate part '%s': %s", name, part, location);
      } else {
        parts.add(part);
      }
    }
  }

  private void validateDuplicateLocation(String name, String location) {
    String otherResource = resourcesByLocation.get(location);
    if (otherResource == null) {
      resourcesByLocation.put(location, name);
    } else {
      error("Resources '%s' and '%s' have the same location: %s", otherResource, name, location);
    }
  }

  private void validateFixedAndVariableLocation(String name, String location) {
    Resource resource = new Resource(name, location);
    for (Entry<String, String> entry : resourcesByLocation.entrySet()) {
      String otherName = entry.getValue();
      if (isLocationChecked(name, otherName)) {
        continue;
      }
      validateFixedAndVariableLocation(resource, new Resource(otherName, entry.getKey()));
    }
  }

  private boolean isLocationChecked(String resource1, String resource2) {
    if (getCheckedResourcesFor(resource1).add(resource2)) {
      getCheckedResourcesFor(resource2).add(resource1);
      return false;
    }
    return true;
  }

  private Collection<String> getCheckedResourcesFor(String resource) {
    Collection<String> result = checkedResourcesByResource.get(resource);
    if (result == null) {
      result = new HashSet<String>();
      result.add(resource);
      checkedResourcesByResource.put(resource, result);
    }
    return result;
  }

  private void validateFixedAndVariableLocation(Resource resource1, Resource resource2) {
    String[] parts1 = resource1.parts();
    String[] parts2 = resource2.parts();
    for (int i = 0; i < parts1.length && i < parts2.length; i++) {
      String part1 = parts1[i];
      String part2 = parts2[i];
      if (part1.equals(part2)) {
        continue;
      }
      boolean isVariable1 = isVariable(part1);
      if (isVariable1 != isVariable(part2) && haveSameMethod(resource1.getName(), resource2.getName()))  {
        warnFixedAndVariableLocation(resource1, resource2, part1, part2, isVariable1);
        break;
      }
      if (!isVariable1) {
        break;
      }
    }
  }

  private boolean haveSameMethod(String resource1, String resource2) {
    Collection<String> methods = new ArrayList<String>();
    for (String method : radl.methodNames(resource1)) {
      methods.add(method);
    }
    for (String method : radl.methodNames(resource2)) {
      if (methods.contains(method)) {
        return true;
      }
    }
    return false;
  }

  private void warnFixedAndVariableLocation(Resource resource1, Resource resource2, String part1, String part2,
      boolean isVariable1) {
    List<Resource> resources = sort(resource1, resource2);

    String fixed = isVariable1 ? part2 : part1;
    String variable = isVariable1 ? part1 : part2;

    warn("Locations of '%s' and '%s' overlap with fixed part '%s' and variable part '%s':\n%s\n%s",
        resources.get(0).getName(), resources.get(1).getName(), fixed, variable, resources.get(0).getUri(),
        resources.get(1).getUri());
  }

  private static List<Resource> sort(Resource... resources) {
    List<Resource> result = new ArrayList<Resource>();
    for (Resource resource : resources) {
      result.add(resource);
    }
    Collections.sort(result);
    return result;
  }

  private void validateMethods(String name) {
    Iterator<String> methodNames = radl.methodNames(name).iterator();
    if (methodNames.hasNext()) {
      while (methodNames.hasNext()) {
        validateMethod(name, methodNames.next());
      }
    } else {
      warn("Resource '%s' has no methods", name);
    }
  }

  private boolean containsAction(String location) {
    for (String part : splitUri(location)) {
      if (isAction(part)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAction(String value) {
    return actions.contains(value);
  }

  private void validateMethod(String resource, String method) {
    validateMethodTransitions(resource, method);
    validateMethodRepresentations(resource, method);
  }

  private void validateMethodRepresentations(String resource, String method) {
    boolean hasRepresentation = false;
    if (validateMethodRepresentations(resource, method, "consumes",
        radl.methodRequestRepresentations(resource, method))) {
      hasRepresentation = true;
    }
    if (validateMethodRepresentations(resource, method, "produces",
        radl.methodResponseRepresentations(resource, method))) {
      hasRepresentation = true;
    }
    if (!"DELETE".equals(method) && !hasRepresentation) {
      warn("Method '%s' in resource '%s' has neither a request nor a response representation", method, resource);
    }
  }

  private boolean validateMethodRepresentations(String resource, String method, String action,
      Iterable<String> methodRepresentations) {
    Iterator<String> representations = methodRepresentations.iterator();
    if (representations.hasNext()) {
      while (representations.hasNext()) {
        assertKnownMediaType(resource, method, action, representations.next());
      }
      return true;
    }
   return false;
  }

  private void assertKnownMediaType(String resource, String method, String action, String representation) {
    for (String mediaType : radl.mediaTypeNames()) {
      if (representation.equals(mediaType)) {
        return;
      }
    }
    warn("Method '%s' in resource '%s' %s undefined media type '%s'", method, resource, action, representation);
  }

  private void validateMethodTransitions(String resource, String method) {
    Iterator<String> transitions = radl.methodTransitions(resource, method).iterator();
    if (transitions.hasNext()) {
      while (transitions.hasNext()) {
        assertImplementedTransitionExists(resource, method, transitions.next());
      }
    } else {
      warn("Method '%s' in resource '%s' implements no transitions", method, resource);
    }
  }

  private void assertImplementedTransitionExists(String resource, String method, String transition) {
    for (String state : radl.stateNames()) {
      for (String name : radl.stateTransitionNames(state)) {
        if (transition.equals(name)) {
          return;
        }
      }
    }
    warn("Method '%s' in resource '%s' implements undefined transition '%s'", method, resource, transition);
  }

  private void validateStateDiagram() {
    Iterator<String> states = radl.stateNames().iterator();
    if (states.hasNext()) {
      boolean startStateFound = false;
      Set<String> stateNames = new TreeSet<String>();
      while (states.hasNext()) {
        String state = states.next();
        if (stateNames.contains(state)) {
          error("Duplicate state: '%s'", state);
        } else {
          stateNames.add(state);
          if (state.isEmpty()) {
            startStateFound = true;
          }
          validateState(state);
        }
      }
      if (!startStateFound) {
        warn("Missing start-state");
      }
    } else {
      warn("Missing start-state");
    }
  }

  private void validateState(String name) {
    Iterator<String> outgoing = radl.stateTransitionNames(name).iterator();
    if (outgoing.hasNext()) {
      while (outgoing.hasNext()) {
        String transition = outgoing.next();
        assertTransitionPointsToKnownState(name, transition);
        assertTransitionIsImplementedBySomeMethod(transition);
        if (!name.isEmpty()) {
          assertTransitionIsDiscoverableByLinkRelation(transition);
        }
      }
    } else {
      assertStateIsPointedToBySomeTransition(name);
    }
  }

  private void assertTransitionIsDiscoverableByLinkRelation(String name) {
    for (String linkRelation : radl.linkRelationNames()) {
      for (String transition : radl.linkRelationTransitions(linkRelation)) {
        if (name.equals(transition)) {
          return;
        }
      }
    }
    warn("Transition '%s' is not discoverable by a link relation", name);
  }

  private void assertTransitionIsImplementedBySomeMethod(String name) {
    for (String resource : radl.resourceNames()) {
      for (String method : radl.methodNames(resource)) {
        for (String transition : radl.methodTransitions(resource, method)) {
          if (name.equals(transition)) {
            return;
          }
        }
      }
    }
    warn("Transition '%s' is not implemented by a method", name);
  }

  private void assertTransitionPointsToKnownState(String state, String transition) {
    for (String to : radl.transitionEnds(transition)) {
      for (String name : radl.stateNames()) {
        if (to.equals(name)) {
          return;
        }
      }
      if (state.isEmpty()) {
        warn("Transition '%s' in start state points to undefined state '%s'", transition, to);
      } else {
        warn("Transition '%s' in state '%s' points to undefined state '%s'", transition, state, to);
      }
    }
  }

  private void assertStateIsPointedToBySomeTransition(String state) {
    for (String from : radl.stateNames()) {
      if (from.equals(state)) {
        continue;
      }
      for (String transition : radl.stateTransitionNames(from)) {
        for (String to : radl.transitionEnds(transition)) {
          if (state.equals(to)) {
            return;
          }
        }
      }
    }
    if (state.isEmpty()) {
      warn("Start state has no transitions");
    } else {
      warn("State '%s' is not connected to any other state", state);
    }
  }

  private void warn(String message, Object... args) {
    message(Level.WARNING, message, args);
  }

  private void message(Level level, String message, Object... args) {
    issues.add(new Issue(getClass(), level, 0, 0, String.format(message, args)));
  }

  private void error(String message, Object... args) {
    message(Level.ERROR, message, args);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }


  private class Resource implements Comparable<Resource> {

    private final String name;
    private final String uri;

    public Resource(String name, String uri) {
      this.name = name;
      this.uri = uri;
    }

    public String getName() {
      return name;
    }

    public String getUri() {
      return uri;
    }

    public String[] parts() {
      return splitUri(uri);
    }

    @Override
    public int compareTo(Resource other) {
      return name.compareTo(other.name);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + name.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Resource)) {
        return false;
      }
      Resource other = (Resource)obj;
      if (!name.equals(other.name)) {
        return false;
      }
      return true;
    }

  }

}
