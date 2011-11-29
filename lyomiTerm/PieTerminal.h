#ifndef __PIE_H__
#define __PIE_H__

#include <sstream>
#include "AsyncSocket.h"

extern unsigned short cp949table[];

#define	COLOR_BLACK		0
#define COLOR_RED		1
#define COLOR_GREEN		2
#define COLOR_YELLOW	3
#define COLOR_BLUE		4
#define COLOR_MAGENTA	5
#define COLOR_CYAN		6
#define COLOR_WHITE		7

typedef struct termchar {
	bool bold;
	bool underscore;
	short foreground;	// foreground color
	short background;	// background color
	int character;	// character (in unicode)
} termchar;

class PieTerminal
{
public:
	PieTerminal();
	~PieTerminal(void);
	
	void connect(const char *host, int port=23);
	void close();
	void update();
	void idle();
private:
	void negotiate(const char * token);
	void send(const char * data, int len=-1);
	char getchar();
	void parse();
	void draw();
	void drawchar(int c);
	void newline();
	char peek();

	bool terminated;
	std::stringstream buffer;
	AsyncSocket *sock;
	int bufferlen;
	termchar screen[24][80];
	char cscreen[24][80];

	int cursorx, cursory;
	int savex, savey;
	int foreground, background;
	bool bold, underscore;

};


#endif