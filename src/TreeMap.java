/*
 * Copyright 2014, Michael T. Goodrich, Roberto Tamassia, Michael H. Goldwasser
 *
 * Developed for use with the book:
 *
 *    Data Structures and Algorithms in Java, Sixth Edition
 *    Michael T. Goodrich, Roberto Tamassia, and Michael H. Goldwasser
 *    John Wiley & Sons, 2014
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.LinkedList;

/**
 * An implementation of a sorted map using a binary search tree.
 *
 * @author Michael T. Goodrich
 * @author Roberto Tamassia
 * @author Michael H. Goldwasser
 */
public class TreeMap extends AbstractSortedMap {

  //---------------- nested BalanceableBinaryTree class ----------------
  /**
   * A specialized version of the LinkedBinaryTree class with
   * additional mutators to support binary search tree operations, and
   * a specialized node class that includes an auxiliary instance
   * variable for balancing data.
   */
  protected static class BalanceableBinaryTree
                         extends LinkedBinaryTree {
    //-------------- nested BSTNode class --------------
    // this extends the inherited LinkedBinaryTree.Node class
    protected static class BSTNode extends Node {
      int aux=0;
      BSTNode(Entry e, Node parent, Node leftChild, Node rightChild) {
        super(e, parent, leftChild, rightChild);
      }
      public int getAux() { return aux; }
      public void setAux(int value) { aux = value; }
    } //--------- end of nested BSTNode class ---------

    
    // positional-based methods related to aux field
    public int getAux(Position p) {
      return ((BSTNode) p).getAux();
    }

    public void setAux(Position p, int value) {
      ((BSTNode) p).setAux(value);
    }

    // Override node factory function to produce a BSTNode (rather than a Node)
    @Override
    protected
    Node createNode(Entry e, Node parent,
                            Node left, Node right) {
      return new BSTNode(e, parent, left, right);
    }

    /** Relinks a parent node with its oriented child node. */
    private void relink(Node parent, Node child,
                        boolean makeLeftChild) {
      child.setParent(parent);
      if (makeLeftChild)
        parent.setLeft(child);
      else
        parent.setRight(child);
    }

    /**
     * Rotates Position p above its parent.  Switches between these
     * configurations, depending on whether p is a or p is b.
     *<pre>
     *          b                  a
     *         / \                / \
     *        a  t2             t0   b
     *       / \                    / \
     *      t0  t1                 t1  t2
     *</pre>
     *  Caller should ensure that p is not the root.
     */
    public void rotate(Position p) {
      Node x = validate(p);
      Node y = x.getParent();        // we assume this exists
      Node z = y.getParent();        // grandparent (possibly null)
      if (z == null) {
        root = x;                                // x becomes root of the tree
        x.setParent(null);
      } else
        relink(z, x, y == z.getLeft());          // x becomes direct child of z
      // now rotate x and y, including transfer of middle subtree
      if (x == y.getLeft()) {
        relink(y, x.getRight(), true);           // x's right child becomes y's left
        relink(x, y, false);                     // y becomes x's right child
      } else {
        relink(y, x.getLeft(), false);           // x's left child becomes y's right
        relink(x, y, true);                      // y becomes left child of x
      }
    }

    /**
     *
     * Returns the Position that becomes the root of the restructured subtree.
     *
     * Assumes the nodes are in one of the following configurations:
     *<pre>
     *     z=a                 z=c           z=a               z=c
     *    /  \                /  \          /  \              /  \
     *   t0  y=b             y=b  t3       t0   y=c          y=a  t3
     *      /  \            /  \               /  \         /  \
     *     t1  x=c         x=a  t2            x=b  t3      t0   x=b
     *        /  \        /  \               /  \              /  \
     *       t2  t3      t0  t1             t1  t2            t1  t2
     *</pre>
     * The subtree will be restructured so that the node with key b becomes its root.
     *<pre>
     *           b
     *         /   \
     *       a       c
     *      / \     / \
     *     t0  t1  t2  t3
     *     
     *
     *      
     *      
     *      
     *        6059
     *       /    \
     *      6047    6071        				
     *     /    \
     *    6019   6055	    
     *   /  
     *  d      
     * / \
     *t0 t1     
     *     
     *     
     *</pre>
     * Caller should ensure that x has a grandparent.
     */
    public Position restructure(Position x) {
      Position y = parent(x);
      Position z = parent(y);
      if ((x == right(y)) == (y == right(z))) {   // matching alignments
        rotate(y);                                // single rotation (of y)
        return y;                                 // y is new subtree root
      } else {                                    // opposite alignments
        rotate(x);                                // double rotation (of x)
        rotate(x);
        return x;                                 // x is new subtree root
      }
    }
  } //----------- end of nested BalanceableBinaryTree class -----------

  /** Representation of the underlying tree structure. */
  protected BalanceableBinaryTree tree = new BalanceableBinaryTree();

  /** Constructs an empty map using the natural ordering of keys. */
  public TreeMap() {
    super();                  // the AbstractSortedMap constructor
    tree.addRoot(null);       // create a sentinel leaf as root
  }

  /**
   * Constructs an empty map using the given comparator to order keys.
   * @param comp comparator defining the order of keys in the map
   */
  public TreeMap(Comparator comp) {
    super(comp);              // the AbstractSortedMap constructor
    tree.addRoot(null);       // create a sentinel leaf as root
  }

  /**
   * Returns the number of entries in the map.
   * @return number of entries in the map
   */
  @Override
  public int size() {
    return (tree.size() - 1) / 2;        // only internal nodes have entries
  }

  /** Utility used when inserting a new entry at a leaf of the tree */
  private void expandExternal(Position p, Entry entry) {
    tree.set(p, entry);            // store new entry at p
    tree.addLeft(p, null);         // add new sentinel leaves as children
    tree.addRight(p, null);
  }


  // Some notational shorthands for brevity (yet not efficiency)
  protected Position root() { return tree.root(); }
  protected Position parent(Position p) { return tree.parent(p); }
  protected Position left(Position p) { return tree.left(p); }
  protected Position right(Position p) { return tree.right(p); }
  protected Position sibling(Position p) { return tree.sibling(p); }
  protected boolean isRoot(Position p) { return tree.isRoot(p); }
  protected boolean isExternal(Position p) { return tree.isExternal(p); }
  protected boolean isInternal(Position p) { return tree.isInternal(p); }
  protected void set(Position p, Entry e) { tree.set(p, e); }
  protected Entry remove(Position p) { return tree.remove(p); }
  protected void rotate(Position p) { tree.rotate(p); }
  protected Position restructure(Position x) { return tree.restructure(x); }

  /**
   * Returns the position in p's subtree having the given key (or else the terminal leaf).
   * @param key  a target key
   * @param p  a position of the tree serving as root of a subtree
   * @return Position holding key, or last node reached during search
   */
  private Position treeSearch(Position p, Integer key) {
    if (isExternal(p))
    {
      //++time;
      return p;                          // key not found; return the final leaf
    }
    
    int comp = compare(key, p.getElement());
    if (comp == 0)
    {
      ++time;
      return p;                          // key found; return its position
    }
    else if (comp < 0)
    {
    	++time;
      return treeSearch(left(p), key);   // search left subtree
    }
    else
    {
    	++time;
      return treeSearch(right(p), key);  // search right subtree
    }
  }
  


  private void printLevelOrder(Position root)
  {
	  int indent = 32;
	  
	  if(root == null)
		  return;
	  
	  LinkedQueue q = new LinkedQueue();
	  
	  q.enqueue(root);
	  
	  while(true)
	  {
		  int nodeCount = q.size();
	
		  if(nodeCount == 0)
			  break;
		  
		  while(nodeCount > 0)
		  {
			  Position p = q.first();
			 
			  if(indent == 32)  
			  {
				  for(int i =0; i < indent-6;i++)
					  System.out.print(" "); 
				  
			  }
			  else
				  for(int i =0; i < indent;i++)
					  System.out.print(" ");
			  
			  if(p.getElement() != null)
					  System.out.print(p.getElement().getKey() +" ");
			  else
				  System.out.print(" X ");
			  
			  q.dequeue();
			  
			  if(left(p) != null)
				  q.enqueue(left(p));
			  if((right(p) != null))
				  q.enqueue(right(p));
				  
			  nodeCount--;
		  }
		  
		  System.out.println();
		  indent /= 2;
	  }
  }

  public void drawTree()
  {
	 printLevelOrder(root());
  }

  /**
   * Returns position with the minimal key in the subtree rooted at Position p.
   * @param p  a Position of the tree serving as root of a subtree
   * @return Position with minimal key in subtree
   */
  protected Position treeMin(Position p) {
    Position walk = p;
    while (isInternal(walk))
      walk = left(walk);
    return parent(walk);              // we want the parent of the leaf
  }

  /**
   * Returns the position with the maximum key in the subtree rooted at p.
   * @param p  a Position of the tree serving as root of a subtree
   * @return Position with maximum key in subtree
   */
  protected Position treeMax(Position p) {
    Position walk = p;
    while (isInternal(walk))
      walk = right(walk);
    return parent(walk);              // we want the parent of the leaf
  }
  
  


  /**
   * Returns the value associated with the specified key, or null if no such entry exists.
   * @param key  the key whose associated value is to be returned
   * @return the associated value, or null if no such entry exists
   */
  @Override
  public County get(Integer key) throws IllegalArgumentException {
    checkKey(key);                          // may throw IllegalArgumentException
    Position p = treeSearch(root(), key);

    rebalanceAccess(p);                     // hook for balanced tree subclasses
    if (isExternal(p)) return null;         // unsuccessful search
    
    return p.getElement().getValue();       // match found
  }

  int time = 0;
  
  public County getTime(Integer key) throws IllegalArgumentException {
	    checkKey(key);                          // may throw IllegalArgumentException
	    time = 0;
	    Position p = treeSearch(root(), key);
	    
	    rebalanceAccess(p);                     // hook for balanced tree subclasses
	    if (isExternal(p)) return null;         // unsuccessful search
	    
	    System.out.println("It took: " + time + " milliseconds" );
	    time = 0;
	    
	    return p.getElement().getValue();       // match found
	  }
    
  
  /**
   * Associates the given value with the given key. If an entry with
   * the key was already in the map, this replaced the previous value
   * with the new one and returns the old value. Otherwise, a new
   * entry is added and null is returned.
   * @param key    key with which the specified value is to be associated
   * @param value  value to be associated with the specified key
   * @return the previous value associated with the key (or null, if no such entry)
   */
  @Override
  public County put(Integer key, County value) throws IllegalArgumentException {
    checkKey(key);                          // may throw IllegalArgumentException
    time = 0;
    Entry newEntry = new MapEntry(key, value);
    Position p = treeSearch(root(), key);
    
    
    if (isExternal(p)) {                    // key is new
      expandExternal(p, newEntry);
      rebalanceInsert(p);                   // hook for balanced tree subclasses
      return null;
    } else {                                // replacing existing key
      County old = p.getElement().getValue();
      set(p, newEntry);
      rebalanceAccess(p);                   // hook for balanced tree subclasses
      return old;
    }
  }

  
  public County putTime(int key, County value) throws IllegalArgumentException {
	    checkKey(key);                          // may throw IllegalArgumentException
	    time = 0;
	    Entry newEntry = new MapEntry(key, value);
	    Position p = treeSearch(root(), key);
	    
	    System.out.println("It took: " + time + " milliseconds");
	    
	    if (isExternal(p)) {                    // key is new
	      expandExternal(p, newEntry);
	      rebalanceInsert(p);                   // hook for balanced tree subclasses
	      return null;
	    } else {                                // replacing existing key
	      County old = p.getElement().getValue();
	      set(p, newEntry);
	      rebalanceAccess(p);                   // hook for balanced tree subclasses
	      return old;
	    }
	  }
  
  
  /**
   * Removes the entry with the specified key, if present, and returns
   * its associated value. Otherwise does nothing and returns null.
   * @param key  the key whose entry is to be removed from the map
   * @return the previous value associated with the removed key, or null if no such entry exists
   */
  @Override
  public County remove(Integer key) throws IllegalArgumentException {
    checkKey(key);                          // may throw IllegalArgumentException
    
    time = 0;
    
    Position p = treeSearch(root(), key);
    System.out.println("It took: " + time + " miliseconds");
    
    time =0;
    
    if (isExternal(p)) {                    // key not found
      rebalanceAccess(p);                   // hook for balanced tree subclasses
      return null;
    } else {
      County old = p.getElement().getValue();
      if (isInternal(left(p)) && isInternal(right(p))) { // both children are internal
        Position replacement = treeMax(left(p));
        set(p, replacement.getElement());
        p = replacement;
      } // now p has at most one child that is an internal node
      Position leaf = (isExternal(left(p)) ? left(p) : right(p));
      Position sib = sibling(leaf);
      remove(leaf);
      remove(p);                            // sib is promoted in p's place
      rebalanceDelete(sib);                 // hook for balanced tree subclasses
      return old;
    }
  }

  // additional behaviors of the SortedMap interface
  /**
   * Returns the entry having the least key (or null if map is empty).
   * @return entry with least key (or null if map is empty)
   */
  @Override
  public Entry firstEntry() {
    if (isEmpty()) return null;
    return treeMin(root()).getElement();
  }

  /**
   * Returns the entry having the greatest key (or null if map is empty).
   * @return entry with greatest key (or null if map is empty)
   */
  @Override
  public Entry lastEntry() {
    if (isEmpty()) return null;
    return treeMax(root()).getElement();
  }

  /**
   * Returns the entry with least key greater than or equal to given key
   * (or null if no such key exists).
   * @return entry with least key greater than or equal to given (or null if no such entry)
   * @throws IllegalArgumentException if the key is not compatible with the map
   */
  @Override
  public Entry ceilingEntry(Integer key) throws IllegalArgumentException {
    checkKey(key);                              // may throw IllegalArgumentException
    Position p = treeSearch(root(), key);
    if (isInternal(p)) return p.getElement();   // exact match
    while (!isRoot(p)) {
      if (p == left(parent(p)))
        return parent(p).getElement();          // parent has next greater key
      else
        p = parent(p);
    }
    return null;                                // no such ceiling exists
  }

  /**
   * Returns the entry with greatest key less than or equal to given key
   * (or null if no such key exists).
   * @return entry with greatest key less than or equal to given (or null if no such entry)
   * @throws IllegalArgumentException if the key is not compatible with the map
   */
  @Override
  public Entry floorEntry(Integer key) throws IllegalArgumentException {
    checkKey(key);                              // may throw IllegalArgumentException
    Position p = treeSearch(root(), key);
    if (isInternal(p)) return p.getElement();   // exact match
    while (!isRoot(p)) {
      if (p == right(parent(p)))
        return parent(p).getElement();          // parent has next lesser key
      else
        p = parent(p);
    }
    return null;                                // no such floor exists
  }

  /**
   * Returns the entry with greatest key strictly less than given key
   * (or null if no such key exists).
   * @return entry with greatest key strictly less than given (or null if no such entry)
   * @throws IllegalArgumentException if the key is not compatible with the map
   */
  @Override
  public Entry lowerEntry(Integer key) throws IllegalArgumentException {
    checkKey(key);                              // may throw IllegalArgumentException
    Position p = treeSearch(root(), key);
    if (isInternal(p) && isInternal(left(p)))
      return treeMax(left(p)).getElement();     // this is the predecessor to p
    // otherwise, we had failed search, or match with no left child
    while (!isRoot(p)) {
      if (p == right(parent(p)))
        return parent(p).getElement();          // parent has next lesser key
      else
        p = parent(p);
    }
    return null;                                // no such lesser key exists
  }

  /**
   * Returns the entry with least key strictly greater than given key
   * (or null if no such key exists).
   * @return entry with least key strictly greater than given (or null if no such entry)
   * @throws IllegalArgumentException if the key is not compatible with the map
   */
  @Override
  public Entry higherEntry(Integer key) throws IllegalArgumentException {
    checkKey(key);                               // may throw IllegalArgumentException
    Position p = treeSearch(root(), key);
    if (isInternal(p) && isInternal(right(p)))
      return treeMin(right(p)).getElement();     // this is the successor to p
    // otherwise, we had failed search, or match with no right child
    while (!isRoot(p)) {
      if (p == left(parent(p)))
        return parent(p).getElement();           // parent has next lesser key
      else
        p = parent(p);
    }
    return null;                                 // no such greater key exists
  }

  // Support for iteration
  /**
   * Returns an iterable collection of all key-value entries of the map.
   *
   * @return iterable collection of the map's entries
   */
  @Override
  public Iterable<Entry> entrySet() {
    ArrayList buffer = new ArrayList<>(size());
    for (Position p : tree.inorder())
      if (isInternal(p)) buffer.add(p.getElement());
    return buffer;
  }

  /**
   * Returns an iterable containing all entries with keys in the range from
   * <code>fromKey</code> inclusive to <code>toKey</code> exclusive.
   * @return iterable with keys in desired range
   * @throws IllegalArgumentException if <code>fromKey</code> or <code>toKey</code> is not compatible with the map
   */
  @Override
  public Iterable<Entry> subMap(Integer fromKey, Integer toKey) throws IllegalArgumentException {
    checkKey(fromKey);                                // may throw IllegalArgumentException
    checkKey(toKey);                                  // may throw IllegalArgumentException
    ArrayList buffer = new ArrayList<>(size());
    if (compare(fromKey, toKey) < 0)                  // ensure that fromKey < toKey
      subMapRecurse(fromKey, toKey, root(), buffer);
    return buffer;
  }

  
  // utility to fill subMap buffer recursively (while maintaining order)
  private void subMapRecurse(Integer fromKey, Integer toKey, Position p,
                              ArrayList buffer) {
    if (isInternal(p))
      if (compare(p.getElement(), fromKey) < 0)
        // p's key is less than fromKey, so any relevant entries are to the right
        subMapRecurse(fromKey, toKey, right(p), buffer);
      else {
        subMapRecurse(fromKey, toKey, left(p), buffer); // first consider left subtree
        if (compare(p.getElement(), toKey) < 0) {       // p is within range
          buffer.add(p.getElement());                      // so add it to buffer, and consider
          subMapRecurse(fromKey, toKey, right(p), buffer); // right subtree as well
        }
      }
  }

  // Stubs for balanced search tree operations (subclasses can override)
  /**
   * Rebalances the tree after an insertion of specified position.  This
   * version of the method does not do anything, but it can be
   * overridden by subclasses.
   * @param p the position which was recently inserted
   */
  protected void rebalanceInsert(Position p) { }

  /**
   * Rebalances the tree after a child of specified position has been
   * removed.  This version of the method does not do anything, but it
   * can be overridden by subclasses.
   * @param p the position of the sibling of the removed leaf
   */
  protected void rebalanceDelete(Position p) { }

  /**
   * Rebalances the tree after an access of specified position.  This
   * version of the method does not do anything, but it can be
   * overridden by a subclasses.
   * @param p the Position which was recently accessed (possibly a leaf)
   */
  protected void rebalanceAccess(Position p) { }

  // remainder of class is for debug purposes only
  /** Prints textual representation of tree structure (for debug purpose only). */
  protected void dump() {
    dumpRecurse(root(), 0);
  }

  /** This exists for debugging only */
  private void dumpRecurse(Position p, int depth) {
    String indent = (depth == 0 ? "" : String.format("%" + (2*depth) + "s", ""));
    if (isExternal(p))
      System.out.println(indent + "leaf");
    else {
      System.out.println(indent + p.getElement());
      dumpRecurse(left(p), depth+1);
      dumpRecurse(right(p), depth+1);
    }
  }

}
