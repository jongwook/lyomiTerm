//
//  lyomiTermView.h
//  lyomiTerm
//
//

#import <Cocoa/Cocoa.h>
#include "PieConnection.h"

@interface lyomiTermView : NSView {
	
	PieConnection *pie;
	NSFont *font;
	CGColorRef defaultForeground, defaultBackground, cursorColor;
	CGColorRef colors[8];
}

@property (nonatomic,retain) PieConnection *pie;

@end
