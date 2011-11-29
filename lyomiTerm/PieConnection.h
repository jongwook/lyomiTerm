//
//  PieConnection.h
//  Pie
//
//

#import <Cocoa/Cocoa.h>
#import "AsyncSocket.h"

#define TERMINAL_ROWS 24
#define TERMINAL_COLS 80

BOOL isCP949(unsigned char a, unsigned char b);

@class PieView, PieViewController;

@interface PieConnection : NSObject {
	PieViewController *viewController;
	PieView *pieView;
	
	AsyncSocket *socket;
	NSStringEncoding encoding;
	NSError *error;
	unichar screen[TERMINAL_ROWS][TERMINAL_COLS];
	int foreground[TERMINAL_ROWS][TERMINAL_COLS];
	int background[TERMINAL_ROWS][TERMINAL_COLS];
	int currentRow, currentCol;
	int savedRow, savedCol;
	int currentForeground, currentBackground;
	BOOL reversed;
	int pos;
	int stage;	// 0:notconnected 1:negotiating 2:connected
}

@property (readonly,assign) NSStringEncoding encoding;
@property (readonly,assign) unichar *screen;
@property (readonly,assign) int *foreground;
@property (readonly,assign) int *background;
@property (readonly,assign) int currentRow, currentCol;
@property (nonatomic,retain) PieView *pieView;
@property (nonatomic,retain) PieViewController *viewController;

-(BOOL) connectToHost:(NSString *)host;
-(BOOL) connectToHost:(NSString *)host onPort:(int)port;

-(void) onSocketDidDisconnect:(AsyncSocket *)sock;
-(void) onSocket:(AsyncSocket *)sock willDisconnectWithError:(NSError *)err;
-(void) onSocket:(AsyncSocket *)sock didConnectToHost:(NSString *)host port:(UInt16)port;
-(void) onSocket:(AsyncSocket *)sock didReadData:(NSData *)data withTag:(long)tag;
-(void) onSocket:(AsyncSocket *)sock didWriteDataWithTag:(long)tag;

-(void) parse;
-(char) getchar;
-(void) negotiate:(const char *)token;
-(void) send:(const char *)token;
-(void) send:(const char *)token length:(int)length;

-(void) drawChar:(unichar)c;
-(void) newline;

@end
