/*
 *  Korean.h
 *  Pie
 *
 *
 */


// Korean automata

#include <map>

class Korean {
protected:
	unichar buffer[8];
	int size;
	int state;

	// make a character until pos, return it, and shift the buffer to the front
	unichar commit(int pos, int next = -1);
	
	std::map<int,int> cho, vowel, jong, complex;
	
public:
	Korean();
	int getState() { return state; }
	
	// added a keystroke,
	unichar add(unichar c);	
	
	// return current character
	unichar value(int pos = -1);
	
	// return current character and clear current buffer 
	unichar clear();
};