package app.meetling.io;

import java.util.List;

/**
 * Makes an <code>Object</code> editable and stores a list of authors. The <code>AuthorType</code>
 * determines how the author data is stored.
 */
interface Editable<AuthorType> {
    List<AuthorType> getAuthors();
}
